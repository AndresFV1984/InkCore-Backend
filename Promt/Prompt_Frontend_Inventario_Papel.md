# Prompt Frontend — Inventario de papel (stock + remanentes)

> Copia el bloque de abajo y úsalo en el agente/chat del frontend InkCore.
> Backend alineado con OpenAPI **0.0.73+**.
> Fuente de verdad: Swagger del backend + este contrato. NO inventes endpoints ni campos.

---

## PROMPT (copiar desde aquí)

```
Trabajas en el frontend de InkCore/Indicolors (litografía). El backend ya
expone inventario de papel en dos recursos distintos bajo el catálogo papers.
Debes crear tipos, API clients, pantallas/dashboards y menús. NO inventes
endpoints ni campos: sigue EXACTAMENTE este contrato.

═══════════════════════════════════════════════════════════════════════════
1. MODELO (obligatorio — no mezclar conceptos)
═══════════════════════════════════════════════════════════════════════════

A) paper_stock = lotes de pliegos de COMPRA
   - Heredan el formato del papel (papers.width × height × unit).
   - NO tienen medidas propias.
   - Sirven para inventario de pliegos comprados / en bodega.
   - Campos API:
     paperStockId, paperId, quantityInitial, quantityAvailable, unitCost,
     entryDate, state
   - state: true=activo/disponible, false=inactivo (soft).
   - Sin consumo automático aún: CRUD manual de cantidades.

B) paper_remnants = remanentes REUTILIZABLES de corte
   - Mismo material del papel origen (paperId), pero con width/height/unit
     PROPIOS (medidas del sobrante, distintas al pliego de compra).
   - NO crear un paper nuevo por cada remanente.
   - NO guardar remanentes en paper_stock.
   - Campos API:
     paperRemnantId, paperId, width, height, unit,
     quantityInitial, quantityAvailable, unitCost (default 0),
     sourceProductionOrderId?, sourcePaperRowId?, entryDate, note, state
   - unit ∈ { "cm", "mm", "in" }; default "cm".
   - Consumo en OP aún NO cableado: solo CRUD de remanentes.

C) Relación con el catálogo + política de remanentes (en papers)
   - Ambos recursos son anidados: /api/v1/papers/{paperId}/stock|remnants
   - Backend al crear/actualizar remanente: solo exige que el paperId exista.
     NO valida acceptsRemnants ni medidas mínimas (casos especiales pueden
     quedar por debajo del mínimo).
   - Política en papers = guía de UI (no hard-block del API de remanentes):
     * acceptsRemnants, minRemnantWidth, minRemnantHeight, minRemnantUnit
     * unit del papel = pliego; minRemnantUnit = unidad de las mínimas
   - Selector sugerido: GET /api/v1/papers?state=true&acceptsRemnants=true
   - Validar en FRONTEND (con override/confirmación para excepciones):
     * preferir papeles con acceptsRemnants=true
     * avisar si medidas < mínimas o > formato de compra (permitir forzar)
   - Para dashboards globales: listar papeles y cargar stock/remnants.
     No hay endpoint agregado company-wide: KPIs en frontend.

═══════════════════════════════════════════════════════════════════════════
2. ENDPOINTS (JWT Bearer en todos; envelope { headers, timestamp, data })
═══════════════════════════════════════════════════════════════════════════

Base papeles:
GET    /api/v1/papers?state=true          // catálogo para selector / cruce
GET    /api/v1/papers/{paperId}

— Stock —
POST   /api/v1/papers/{paperId}/stock/register
PUT    /api/v1/papers/{paperId}/stock/update/{paperStockId}
GET    /api/v1/papers/{paperId}/stock/{paperStockId}
GET    /api/v1/papers/{paperId}/stock?state=
DELETE /api/v1/papers/{paperId}/stock/{paperStockId}

Body create/update stock:
{
  "quantityInitial": 1000.00,      // required
  "quantityAvailable": 1000.00,    // required; <= initial
  "unitCost": 1500.00,             // required >= 0
  "entryDate": "2026-10-06",       // opcional; default hoy backend
  "state": true                    // opcional; default true
}

Response stock:
{
  "paperStockId", "paperId",
  "quantityInitial", "quantityAvailable", "unitCost",
  "entryDate", "state"
}

— Remanentes —
POST   /api/v1/papers/{paperId}/remnants/register
PUT    /api/v1/papers/{paperId}/remnants/update/{paperRemnantId}
GET    /api/v1/papers/{paperId}/remnants/{paperRemnantId}
GET    /api/v1/papers/{paperId}/remnants?state=
DELETE /api/v1/papers/{paperId}/remnants/{paperRemnantId}

Body create remnant:
{
  "width": 35.00,                  // required > 0
  "height": 50.00,                 // required > 0
  "unit": "cm",                    // opcional cm|mm|in
  "quantityInitial": 12.00,        // required
  "quantityAvailable": 12.00,      // opcional en create (default=initial)
  "unitCost": 0.00,                // opcional; default 0
  "sourceProductionOrderId": null, // opcional (trazabilidad OP)
  "sourcePaperRowId": null,        // opcional (fila de corte)
  "entryDate": "2026-10-06",
  "note": "Sobrante de corte Bond 70x100",
  "state": true
}

Body update remnant: mismos campos; quantityAvailable es required.

Response remnant:
{
  "paperRemnantId", "paperId",
  "width", "height", "unit",
  "quantityInitial", "quantityAvailable", "unitCost",
  "sourceProductionOrderId", "sourcePaperRowId",
  "entryDate", "note", "state"
}

Roles: ADMINISTRADOR | OPERADOR.
Errores: envelope { headers, timestamp, path, message, errors }.
DELETE responde 204 sin body.

═══════════════════════════════════════════════════════════════════════════
3. NAVEGACIÓN / RUTAS SUGERIDAS
═══════════════════════════════════════════════════════════════════════════

Menú (grupo Inventario o Papeles — alinear con menú existente):
  - Inventario de pliegos   → /papers/stock  (o /inventory/paper-stock)
  - Remanentes reutilizables → /papers/remnants (o /inventory/paper-remnants)

También accesible desde detalle de un papel:
  - Tab “Inventario”  → CRUD stock del paperId
  - Tab “Remanentes”  → CRUD remanentes del paperId

NO crear módulos legacy paper-sheets / paper-types.

═══════════════════════════════════════════════════════════════════════════
4. DASHBOARD 1 — Inventario de pliegos (paper_stock)
═══════════════════════════════════════════════════════════════════════════

Objetivo: ver y gestionar lotes de pliegos de compra por papel.

Layout (una composición operativa, no un muro de cards):
1) Cabecera: título “Inventario de pliegos” + filtro estado (Todos|Activos|Inactivos)
   + selector de papel (required para listar; o “todos” si cargas en paralelo).
2) KPIs (calculados en UI a partir del listado cargado; sin endpoint agregado):
   - Total pliegos disponibles (Σ quantityAvailable de state=true)
   - Valor inventario (Σ quantityAvailable * unitCost)
   - # lotes activos
   - # papeles con stock bajo (definir umbral UI configurable, ej. available < 100)
3) Tabla principal por lote:
   Papel (name + formato del catálogo) | Lote | Inicial | Disponible |
   Costo unit. | Valor lote (available*unitCost) | Ingreso | Estado | Acciones
4) Acciones: Nuevo lote | Editar | Activar/Inactivar (update state) | Eliminar
5) Formulario modal/drawer create/update con validaciones:
   - quantityAvailable <= quantityInitial
   - unitCost >= 0
   - paperId obligatorio (selector del catálogo papers state=true)
   - Al crear: quantityAvailable default = quantityInitial; state default true

Estados vacíos: mensaje claro si el papel no tiene lotes.
Loading/error según patrones del proyecto.

═══════════════════════════════════════════════════════════════════════════
5. DASHBOARD 2 — Remanentes reutilizables (paper_remnants)
═══════════════════════════════════════════════════════════════════════════

Objetivo: inventario de sobrantes de corte útiles para trabajos futuros.

Layout:
1) Cabecera: “Remanentes reutilizables” + filtro state + selector de papel
   + búsqueda local por medidas / nota.
2) KPIs (UI):
   - # remanentes activos
   - Σ quantityAvailable activos
   - # formatos distintos (agrupar width×height×unit)
   - Valor opcional Σ available*unitCost (puede ser 0 si no valorizan)
3) Tabla:
   Papel origen (name, grammage, coated) | Medida remanente (W×H unit) |
   Inicial | Disponible | Costo | OP origen (si hay) | Nota | Ingreso |
   Estado | Acciones
4) Acciones: Nuevo remanente | Editar | Activar/Inactivar | Eliminar
5) Formulario:
   - paperId (selector) — muestra formato de COMPRA del papel solo como
     referencia; los campos width/height/unit del form son del REMANENTE
   - Validar width>0, height>0, available<=initial
   - unitCost default 0
   - note opcional
   - sourceProductionOrderId / sourcePaperRowId: opcionales; si el proyecto
     aún no tiene picker de OP, dejar inputs/ocultos o omitir en UI v1
     (enviar null). No inventes endpoints de búsqueda de OP para esto.

Badge visual: “Remanente” vs “Pliego” para no confundir con stock.
Mostrar claramente que la medida del remanente ≠ medida del papel catálogo.

═══════════════════════════════════════════════════════════════════════════
6. TIPOS / API CLIENT (TypeScript — adaptar a convención del repo)
═══════════════════════════════════════════════════════════════════════════

export type PaperStock = {
  paperStockId: string;
  paperId: string;
  quantityInitial: number;
  quantityAvailable: number;
  unitCost: number;
  entryDate: string; // YYYY-MM-DD
  state: boolean;
};

export type PaperRemnant = {
  paperRemnantId: string;
  paperId: string;
  width: number;
  height: number;
  unit: "cm" | "mm" | "in";
  quantityInitial: number;
  quantityAvailable: number;
  unitCost: number;
  sourceProductionOrderId: string | null;
  sourcePaperRowId: string | null;
  entryDate: string;
  note: string | null;
  state: boolean;
};

Clients sugeridos:
- listPaperStock(paperId, state?)
- getPaperStock(paperId, paperStockId)
- registerPaperStock(paperId, body)
- updatePaperStock(paperId, paperStockId, body)
- deletePaperStock(paperId, paperStockId)
- listPaperRemnants(paperId, state?)
- getPaperRemnant(paperId, paperRemnantId)
- registerPaperRemnant(paperId, body)
- updatePaperRemnant(paperId, paperRemnantId, body)
- deletePaperRemnant(paperId, paperRemnantId)

Desanidar siempre data del envelope de éxito.

═══════════════════════════════════════════════════════════════════════════
7. UX / REGLAS DE PRODUCTO
═══════════════════════════════════════════════════════════════════════════

- Stock y remanentes son pantallas hermanas, no la misma tabla.
- En detalle de papel: tabs Inventario | Remanentes (además de precios/despieces
  si ya existen).
- Filtro state por defecto: Activos (state=true).
- Eliminar: confirmar; preferir inactivar (state=false) cuando el lote aún
  tiene historial operativo — pero el backend permite DELETE físico: úsalo
  solo con confirmación fuerte.
- No inventar movimientos de kardex ni consumo desde OP en esta entrega.
- No usar paperSheetId / paperTypeId (eliminados).
- i18n/labels en español (Indicolors): “Inventario de pliegos”,
  “Remanentes reutilizables”, “Disponible”, “Costo unitario”, etc.
- Seguir design system / patrones de listados existentes del frontend
  (tablas, drawers, toasts, auth). No imponer un look genérico nuevo.

═══════════════════════════════════════════════════════════════════════════
8. CHECKLIST DE ACEPTACIÓN
═══════════════════════════════════════════════════════════════════════════

[ ] Menú + rutas para dashboard stock y dashboard remanentes
[ ] Tabs o secciones en detalle de papel para ambos CRUDs
[ ] API clients tipados; envelope data desanidado
[ ] CRUD completo stock (register/update/get/list/delete) con state
[ ] CRUD completo remanentes con medidas propias y state
[ ] Listados con filtro ?state=
[ ] KPIs calculados en UI (sin endpoints agregados inventados)
[ ] Validaciones available <= initial; costos >= 0; medidas > 0
[ ] UI deja claro: remanente ≠ pliego de compra (medidas distintas)
[ ] Sin paper-types / paper-sheets / paper-cost legacy
[ ] Roles: solo usuarios autenticados ADMIN/OPERADOR según backend
```

---

## Notas para quien pega el prompt

- Context path típico: `/InkCore-backend` (configurable).
- Si el frontend ya tiene el catálogo de papeles, reutiliza el selector `GET /api/v1/papers`.
- Esta entrega **no** cablea consumo de stock/remanentes en el wizard de OP.
```
