# Prompts para incorporar costeo por hora-máquina y mermas a InkCore/Indicolors

> Contexto general (aplica a los 3 prompts): sistema `indicolors` sobre PostgreSQL,
> multi-tenant por `company_id`, ya tiene 43 tablas cubriendo Especificaciones,
> Preprensa, Corte de papel, Impresión, Terminados, Acabados, Cobro/CxC y un
> módulo "Estación" de captura de eventos en planta. **No existe** tabla de
> máquinas, costo/hora, ni ningún campo de merma o desperdicio con costo
> asociado (solo un campo manual `manual_surplus` sin valorizar).
> Objetivo: agregar (1) catálogo de máquinas con costo/hora real, (2) registro
> de mermas/desperdicios valorizados en cada operación, y (3) un reporte que
> compare costo cotizado vs. costo real por Orden de Producción para conocer
> la ganancia neta real.

---

## PROMPT 1 — BASE DE DATOS (PostgreSQL)

```
Trabajas sobre el schema `indicolors` de una base de datos PostgreSQL para un
sistema de órdenes de producción litográfico (InkCore/Indicolors). Debes
escribir un script SQL de migración (nuevo archivo, no modifiques el script
original) que agregue costeo por hora-máquina y control de mermas/desperdicios,
siguiendo EXACTAMENTE las convenciones ya usadas en el schema:

- IDs: CHARACTER VARYING(64) NOT NULL DEFAULT gen_random_uuid()::text
- Toda tabla lleva company_id CHARACTER VARYING(64) NOT NULL con FK a
  indicolors.companies (multi-tenant)
- Timestamps: TIMESTAMP WITHOUT TIME ZONE, created_at/updated_at DEFAULT now()
- CONSTRAINT con nombre explícito (pkey, fk, check) por tabla
- Índices explícitos por company_id, por FKs frecuentes y por (company_id, state)
- COMMENT ON TABLE y COMMENT ON COLUMN para cada tabla y columna nueva
- GRANT ALL PRIVILEGES ... TO indicolors_owner; GRANT SELECT, INSERT, UPDATE,
  DELETE ... TO indicolors_app; al final de cada tabla
- Patrón "snapshot": cuando una fila de una orden referencia un catálogo
  (ej. production_order_paper_rows guarda paper_name, sheet_value como
  snapshot de papers), replica el mismo patrón para máquinas
- Patrón de tabla derivada con trigger (ver accounts_receivable +
  fn_sync_accounts_receivable_delivery/payment): para el resumen de costos,
  usa una tabla derivada mantenida por trigger, no calculada solo en la app

Crea las siguientes tablas nuevas:

1. indicolors.machines (catálogo de máquinas por compañía)
   - machine_id, company_id, name, machine_type
     (CHECK IN ('preprensa','corte','impresion','terminados','acabados'))
   - manufacturer, model (opcionales)
   - purchase_cost NUMERIC(14,2) NOT NULL CHECK >= 0
   - useful_life_years NUMERIC(5,2) NOT NULL CHECK > 0
   - annual_maintenance_cost NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK >= 0
   - monthly_operator_cost NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK >= 0
     (costo mensual del operario asignado a esta máquina; puede ser 0 si el
     operario ya se costea en otro lado)
   - energy_cost_per_hour NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK >= 0
   - productive_hours_per_year NUMERIC(10,2) NOT NULL CHECK > 0
     (horas REALES de producción al año, no horas de calendario; comentario
     de columna debe advertir explícitamente esto)
   - cost_per_hour NUMERIC(12,2) NOT NULL DEFAULT 0
     (cacheado, se recalcula por trigger BEFORE INSERT/UPDATE cuando cambian
     los campos de costo, con la fórmula:
     cost_per_hour = (purchase_cost/useful_life_years + annual_maintenance_cost
       + monthly_operator_cost*12) / productive_hours_per_year
       + energy_cost_per_hour)
   - state BOOLEAN DEFAULT TRUE, creation_date DATE DEFAULT CURRENT_DATE,
     updated_at TIMESTAMP DEFAULT now()
   - UNIQUE (company_id, name)

2. indicolors.machine_cost_history (auditoría de recalculo, para trazar
   cambios de tarifa en el tiempo — la industria recomienda recalcular cada
   trimestre)
   - machine_cost_history_id, company_id, machine_id (FK), cost_per_hour,
     purchase_cost, annual_maintenance_cost, monthly_operator_cost,
     energy_cost_per_hour, productive_hours_per_year (snapshot de todos los
     componentes en ese momento), effective_from TIMESTAMP DEFAULT now(),
     changed_by CHARACTER VARYING(64) (FK a users)
   - Se inserta automáticamente vía trigger AFTER UPDATE en machines cuando
     cost_per_hour cambia

3. indicolors.production_order_machine_usage (uso de máquina por operación
   de una OP; 1 fila por combinación orden+fase+máquina)
   - production_order_machine_usage_id, company_id, production_order_id (FK),
     phase CHARACTER VARYING(32) NOT NULL CHECK IN ('preprensa','corte-papel',
     'impresion','terminados','acabados') (mismos valores que
     station_operation_events.phase, mantener consistencia),
     machine_id (FK a machines), machine_name_snapshot, cost_per_hour_snapshot
   - estimated_setup_minutes INTEGER DEFAULT 0 CHECK >= 0 (tiempo de arranque/
     calibración estimado, ej. pliegos de arranque convertidos a minutos)
   - estimated_run_minutes INTEGER DEFAULT 0 CHECK >= 0
   - estimated_machine_cost NUMERIC(12,2) (calculado: (setup+run)/60 *
     cost_per_hour_snapshot)
   - actual_setup_minutes INTEGER, actual_run_minutes INTEGER
     (se llenan al cierre real de la fase, pueden derivarse de
     station_operation_events inicio_fase/fin_fase o capturarse manual)
   - actual_machine_cost NUMERIC(12,2)
   - created_at, updated_at
   - Comentario explícito: esta tabla es la que faltaba para dejar de
     "costear solo por cantidad solicitada" y sí cargar el costo fijo real
     de tener la máquina prendida en cada operación

4. indicolors.production_order_waste_records (mermas y desperdicios
   valorizados, uno por fase/insumo afectado)
   - production_order_waste_record_id, company_id, production_order_id (FK),
     phase CHARACTER VARYING(32) (mismo dominio que arriba)
   - waste_category CHARACTER VARYING(20) NOT NULL CHECK IN
     ('merma_corte','merma_operativa','merma_administrativa','desperdicio')
     (merma_corte y merma_operativa son planificables/predecibles;
     desperdicio es lo NO planificado, la fuga real de rentabilidad)
   - material_type CHARACTER VARYING(20) NOT NULL CHECK IN
     ('papel','tinta','plancha','acabado','otro')
   - paper_row_id CHARACTER VARYING(64) (FK opcional a
     production_order_paper_rows, cuando aplica a una fila de corte)
   - postpress_line_id CHARACTER VARYING(64) (FK opcional a
     production_order_postpress_lines, cuando aplica a un acabado)
   - planned_quantity NUMERIC(12,2) DEFAULT 0 (unidades/pliegos planificados
     como merma, valorizados a costeo)
   - actual_quantity NUMERIC(12,2) (capturado al cierre real de la fase)
   - unit_cost_snapshot NUMERIC(12,2) NOT NULL (costo unitario del material
     en el momento del registro, snapshot)
   - planned_cost NUMERIC(12,2), actual_cost NUMERIC(12,2) (calculados)
   - note TEXT (motivo, ej. "cambio de spec del cliente", "defecto de
     impresión", ver clasificación de causas típica del sector: papel de
     mala calidad, mal cortado, ajuste de registro, cambio a medio tiro)
   - created_at, updated_at

5. indicolors.production_order_cost_summary (tabla DERIVADA, 1 fila por OP,
   mantenida por triggers — mismo patrón que accounts_receivable)
   - production_order_id (PK, FK a production_orders)
   - company_id
   - estimated_material_cost, estimated_machine_cost, estimated_waste_cost,
     estimated_total_cost NUMERIC(14,2)
   - actual_material_cost, actual_machine_cost, actual_waste_cost,
     actual_total_cost NUMERIC(14,2)
   - quoted_price NUMERIC(14,2) (tomado de production_order_billing_details)
   - estimated_margin, actual_margin NUMERIC(14,2)
   - actual_margin_pct NUMERIC(6,2)
   - updated_at
   - Triggers: AFTER INSERT/UPDATE en production_order_machine_usage y en
     production_order_waste_records deben hacer UPSERT sobre esta tabla,
     recalculando las sumas de esa OP (sigue el patrón de
     fn_sync_accounts_receivable_payment: upsert dentro de la misma
     transacción, nunca dejarlo solo a cargo del backend)

Además, agrega estas columnas (ALTER TABLE) a tablas existentes SIN romper
nada de lo ya construido:
- indicolors.production_order_paper_rows: agrega
  planned_waste_percentage NUMERIC(5,2) DEFAULT 0 CHECK entre 0 y 100
  (para que el cálculo de pliegos a comprar incluya el % de merma esperado
  ANTES de cotizar, no solo el manual_surplus posterior)

Para cada tabla nueva agrega también los índices y comentarios equivalentes
a los que ya existen en tablas análogas del script (por ejemplo,
production_order_paper_rows y production_order_postpress_records son las
mejores referencias de estilo). No toques ni reordenes nada del script
original; el resultado debe ser un archivo de migración adicional,
ejecutable después del script base.
```

---

## PROMPT 2 — BACKEND

```
Backend del sistema InkCore/Indicolors (ajusta el framework/lenguaje al que
ya usa el proyecto; si no lo sabes, pregunta antes de asumir). Ya existe la
migración de base de datos que agrega: indicolors.machines,
indicolors.machine_cost_history, indicolors.production_order_machine_usage,
indicolors.production_order_waste_records e
indicolors.production_order_cost_summary (tabla derivada).

Necesito los siguientes cambios en el backend:

1. MÓDULO CATÁLOGO DE MÁQUINAS (nuevo, sigue el mismo patrón CRUD que ya
   existe para finished_products / plate_types / thousand_rates):
   - Endpoints CRUD para indicolors.machines (crear, editar, listar activas/
     inactivas por company_id, desactivar en vez de borrar — mismo patrón
     `state` boolean del resto del sistema)
   - El cálculo de cost_per_hour NO debe hacerse en el backend si ya está
     como columna calculada/trigger en la BD; el backend solo valida los
     inputs (purchase_cost, useful_life_years, annual_maintenance_cost,
     monthly_operator_cost, energy_cost_per_hour, productive_hours_per_year)
     y deja que la BD calcule. Expón el resultado en la respuesta.
   - Endpoint de "recalcular tarifas" que se pueda ejecutar manualmente o
     programar trimestralmente (cron/job), que dispare el trigger de
     machine_cost_history para dejar auditoría de cada cambio de tarifa.

2. EXTENSIÓN DEL WIZARD DE ORDEN DE PRODUCCIÓN (Preprensa, Corte de papel,
   Impresión, Terminados, Acabados):
   - En cada paso que hoy guarda snapshots de catálogos (ver el patrón en
     production_order_paper_rows y production_order_print_entries: guardan
     nombre y precio del catálogo en el momento de guardar), agrega la
     posibilidad de seleccionar una máquina (indicolors.machines filtrada
     por machine_type == fase actual) y estimar minutos de setup/run.
   - Al guardar, crea/actualiza la fila correspondiente en
     production_order_machine_usage con el snapshot de cost_per_hour de la
     máquina en ese momento (igual que ya hacen con thousand_rates:
     basic_rate_price, basic_rate_name, etc. — snapshot, nunca referencia
     viva que pueda cambiar el costo histórico de una OP ya cotizada).
   - Calcula estimated_machine_cost en el servidor (nunca en el cliente),
     igual que ya se hace con basic_printing_price en
     production_order_print_entries.

3. CAPTURA DE MERMA/DESPERDICIO:
   - Al cotizar (fase Corte de papel): usar production_order_paper_rows.
     planned_waste_percentage para calcular automáticamente
     planned_quantity y planned_cost en production_order_waste_records
     (categoría merma_corte). Rango sugerido por defecto si el usuario no
     lo cambia: 2%-5% para merma_corte, 3%-8% para merma_operativa en
     impresión — exponlos como valores configurables por compañía, no
     hardcodeados, pero con ese rango como sugerencia en la UI.
   - Al cerrar cada fase en el módulo Estación (reutiliza el evento
     'fin_fase' que ya existe en station_operation_events), agrega un campo
     opcional en el payload para capturar actual_quantity de merma/
     desperdicio real observado en esa fase, y persiste en
     production_order_waste_records (categoría 'desperdicio' si excede lo
     planificado, o actualiza actual_quantity de la fila de merma
     planificada si coincide con lo esperado).
   - IMPORTANTE: cuando se registra un desperdicio, también debe
     descontarse/sumarse el costo de máquina y tinta asociado a esas
     unidades desperdiciadas, no solo el material — usa
     production_order_machine_usage de esa misma fase para prorratear.

4. SINCRONIZACIÓN DE production_order_cost_summary:
   - Si decides mantener el upsert por trigger en BD (recomendado, más
     seguro), el backend solo necesita LEER esta tabla, nunca escribirla
     directamente.
   - Si el equipo prefiere hacerlo desde el backend en vez de trigger,
     entonces crea un servicio `recalculateCostSummary(productionOrderId)`
     que se invoque transaccionalmente después de cualquier escritura en
     paper_rows, machine_usage o waste_records, sumando estimado vs real
     y comparando contra production_order_billing_details.quoted_price
     (usa el nombre de columna real de esa tabla en el script).

5. NUEVO ENDPOINT DE REPORTE:
   GET /production-orders/{id}/cost-summary → devuelve
   production_order_cost_summary con el desglose completo (material,
   máquina, merma, estimado vs real, margen y % de margen real)
   GET /reports/profitability?from=&to=&clientId=&sellerId= → lista de
   órdenes con margen real ordenadas de menor a mayor (para detectar
   rápidamente qué trabajos o clientes están dejando pérdida)

Mantén el mismo estilo de autenticación/autorización multi-tenant
(company_id siempre filtrado por el JWT del usuario) y el patrón de
control de concurrencia optimista (campo `version`) que ya usa
production_orders.
```

---

## PROMPT 3 — FRONTEND

```
Frontend del sistema InkCore/Indicolors (ajusta el framework al que ya usa
el proyecto — React/Vue/Angular, etc.; sigue el sistema de diseño y
componentes ya existentes en las pantallas de catálogos como "Tipos de
papel", "Tipos de plancha" y "Tarifas por millar"). Necesito 3 frentes de
trabajo:

1. NUEVA PANTALLA DE CATÁLOGO: "Máquinas"
   - Listado con filtro por tipo (Preprensa, Corte, Impresión, Terminados,
     Acabados) y estado activo/inactivo, igual estructura visual que la
     pantalla de "Tipos de plancha" o "Tarifas por millar" ya existentes.
   - Formulario "Nueva máquina" / "Editar máquina" con los campos:
     nombre, tipo, fabricante/modelo (opcional), costo de compra,
     vida útil (años), mantenimiento anual, costo mensual del operario,
     costo de energía por hora, horas productivas al año.
   - Muestra en tiempo real (mientras el usuario llena el formulario) el
     costo/hora resultante calculado, con un tooltip que explique la
     fórmula: "(Depreciación anual + Mantenimiento + Operario anual) /
     Horas productivas + Energía por hora". Aclara en el formulario, con
     un texto de ayuda visible, que "horas productivas al año" NO son las
     horas de calendario laboral, sino las horas reales de producción
     (descontando mantenimiento, montajes y tiempos muertos) — este es el
     error más común del sector y debe evitarse desde la UI.

2. WIZARD DE ORDEN DE PRODUCCIÓN — agregar selector de máquina y de merma
   en cada paso relevante (Preprensa, Corte de papel, Impresión, Terminados,
   Acabados), siguiendo el mismo patrón visual que ya usan los selectores
   de catálogo existentes (ej. selector de Tipo de papel o Tarifa por
   millar en el paso de Impresión):
   - Selector de máquina (filtrado por el tipo de la fase actual).
   - Campos de minutos de arranque y minutos de producción estimados.
   - Campo de % de merma esperada (con valor sugerido pre-cargado según el
     tipo de fase: ~3% en Corte, ~5% en Impresión, editable).
   - Muestra un resumen de costos por fila/plancha que sume: material +
     costo de máquina estimado + costo de merma estimada — igual que hoy
     ya se muestra total_paper_value y total_cut_value en el paso de Corte
     de papel, agrega estas dos columnas nuevas al mismo resumen.

3. MÓDULO ESTACIÓN (planta) — al marcar "fin de fase" de una operación
   (reutiliza el flujo que ya dispara el evento fin_fase), agrega un campo
   opcional "Registrar merma/desperdicio real" donde el operario ingresa la
   cantidad real desperdiciada en esa fase (pliegos, unidades o piezas,
   según el tipo de material), con un motivo desde una lista corta
   (papel de mala calidad, mal cortado, ajuste de registro/color, cambio a
   medio tiro, defecto de impresión, otro).

4. NUEVA PANTALLA DE REPORTE: "Rentabilidad por Orden"
   - Tabla con: N° de OP, cliente, cantidad, costo cotizado, costo real,
     margen estimado, margen real, % margen real — ordenable, con
     resaltado visual (ej. rojo) cuando el margen real es menor al
     estimado en más de X%, para detectar de un vistazo qué órdenes están
     perdiendo dinero.
   - Vista de detalle por OP que desglosa: costo de material (estimado vs
     real), costo de máquina (estimado vs real) y costo de merma/
     desperdicio (estimado vs real) — un desglose de 3 filas x 2 columnas,
     simple, sin gráficos complejos innecesarios.
   - Filtros por rango de fecha, cliente y vendedor.

Consume los endpoints ya definidos en el backend:
GET /machines, POST /machines, PUT /machines/{id}
GET /production-orders/{id}/cost-summary
GET /reports/profitability

Mantén la consistencia de idioma (español), formato de moneda (COP) y el
patrón de componentes reutilizables que ya existe en el resto del wizard
de Orden de Producción.
```

---

### Nota de uso

Puedes pegar cada prompt por separado en tu herramienta de codificación
(uno para generar la migración SQL, otro para el backend, otro para el
frontend), en ese orden — la base de datos primero, porque backend y
frontend dependen de esas tablas nuevas. Si tu stack real es distinto al
asumido en algún punto (por ejemplo, si no usas triggers para
`production_order_cost_summary` sino un job programado), dile eso a la IA
antes de que empiece a generar código, para que no invente supuestos.
