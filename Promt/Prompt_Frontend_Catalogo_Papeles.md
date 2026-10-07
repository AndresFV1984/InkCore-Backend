# Prompt Frontend — Catálogo de Papeles (modelo simplificado)

> Copia el bloque de abajo y úsalo en el agente/chat del frontend InkCore.
> Backend ya desplegado / alineado con OpenAPI **0.0.73+**.
> Fuente de verdad: Swagger del backend + `docs/CONVENCION_ENDPOINTS.md`.

---

## PROMPT (copiar desde aquí)

```
Trabajas en el frontend de InkCore/Indicolors (litografía). El backend ya
reescribió el módulo de papel a un catálogo simple. Debes adaptar UI, tipos,
servicios/API clients, formularios del wizard de OP (paso Corte de papel) y
rutas/menús. NO inventes endpoints ni campos: sigue EXACTAMENTE este contrato.

═══════════════════════════════════════════════════════════════════════════
1. DECISIONES DE PRODUCTO / MODELO (obligatorias)
═══════════════════════════════════════════════════════════════════════════

A) Un solo catálogo: PAPEL = material + formato
   - Ya NO existe “tipo de papel” separado de “pliego/formato”.
   - Un papel tiene: name, grammage (opcional), width, height, unit, coated,
     acceptsRemnants, minRemnantWidth, minRemnantHeight, minRemnantUnit, state.
   - unit ∈ { "cm", "mm", "in" }; default "cm".
   - coated (boolean): esmaltado vive en el papel, NO en el precio del proveedor.
   - Política de remanentes (configuración del papel; validación de uso en UI):
     * acceptsRemnants=false (default): sugerir no ofrecer el papel en remanentes;
       minRemnantWidth/Height/Unit deben ir null al guardar el papel.
     * acceptsRemnants=true: minRemnantWidth, minRemnantHeight y minRemnantUnit
       obligatorios en el papel (unit default=unit del pliego si se omite).
       minRemnantUnit es independiente de unit del pliego.
     * El API de remanentes NO bloquea por estas reglas (excepciones de planta).
   - Unicidad lógica backend: (companyId, name, grammage, width, height, unit).
   - Listado: GET /api/v1/papers?state=&coated=&acceptsRemnants=

B) Precios por proveedor (vigentes + historial)
   - Tabla/API: precios vigentes por (paperId, supplierId).
   - Campos: sheetValue, packageUnit (obligatorio > 0), freightPerSheet,
     minPurchaseSheets, paymentDays, deliveryDays, priceDate, preferred, state.
   - packageUnit = pliegos por unidad de empaque del proveedor.
   - landedCostPerSheet = sheetValue + freightPerSheet (calculado en backend;
     solo lectura en UI).
   - Solo UN preferred=true activo por papel.
   - Historial: solo lectura (trigger backend); incluye packageUnit.
     No hay POST/PUT de historial.

C) Despieces asociados al papel (paper_cut_layouts)
   - Relación papel ↔ cut_layout (catálogo /api/v1/cut-layouts).
   - Campos: cutLayoutId, orientation ("vertical"|"horizontal"|null),
     wastePercentage, note, state.
   - DECISIÓN CRÍTICA sobre wastePercentage del despiece por papel:
     * Es SOLO sugerencia para prellenar formularios de UI.
     * NO es la merma que usa el backend para calcular la OP.
     * El cálculo de OP usa company_waste_settings + plannedWastePercentage
       enviado en el paso de corte.
   - No hay endpoint “by-piece-size” de sheets; eliminado.

D) Inventario (paper_stock)
   - Lotes por papel: quantityInitial, quantityAvailable, unitCost, entryDate, state.
   - List admite ?state=. Sin consumo automático aún: UI CRUD de lotes.

D2) Remanentes reutilizables (paper_remnants)
   - Sobrantes de corte del mismo papel (material) con width/height/unit propios.
   - NO crear un paper nuevo por cada remanente; NO mezclar con paper_stock.
   - Campos: width, height, unit, quantityInitial, quantityAvailable, unitCost
     (default 0), sourceProductionOrderId?, sourcePaperRowId?, entryDate, note, state.
   - Endpoints anidados bajo /api/v1/papers/{paperId}/remnants
     (register/update/get/list/delete). List admite ?state=.
   - Consumo en OP aún no cableado: solo CRUD de inventario de remanentes.

E) Qué se ELIMINÓ (borrar del frontend)
   - Módulo / rutas /api clients de:
     * paper-types (/api/v1/paper-types…)
     * paper-sheets / pliegos (/api/v1/paper-sheets…)
     * paper-cost / cotizador de papel (/api/v1/paper-cost…)
     * paper-sheet prices, sheet cut-layouts, sheet stock como recursos propios
   - Campos legacy: paperTypeId, paperSheetId, paperSheetCutLayoutId,
     quotes/details de cotización de papel.
   - Tags Swagger “Tipos de papel”, “Pliegos”, “Costo de papel”.
   - Cualquier pantalla de “tipos de papel” o “pliegos” separada: unificar en
     “Papeles”.

F) Órdenes de producción — paso Corte
   - Cada paper_row usa paperId (FK catálogo papers), NUNCA paperSheetId.
   - Opcional: paperCutLayoutId → el backend resuelve cutLayoutId desde
     paper_cut_layouts.
   - Alternativa: enviar cutLayoutId directo del catálogo despieces.
   - supplierId + priceRule definen el precio snapshot:
     priceRule ∈ { "PREFERRED", "REPLACEMENT", "BEST_COST" }.
   - plannedWastePercentage: merma de CORTE de la OP.
     * Si se omite, backend usa default de company_waste_settings (sugerencia
       inicial típica ~2% corte).
     * NO tomar automáticamente paper_cut_layouts.wastePercentage como valor
       de cálculo; solo puedes usarlo para PRELLENAR el input si el usuario
       aún no eligió merma.
   - Snapshots (solo lectura en respuesta): paperName, paperSize, sheetValue,
     packageUnit (desde precio proveedor), cutValue (desde cut_layouts),
     isCoated, freightPerSheetSnapshot, priceDateSnapshot, piecesPerSheet,
     cutLayoutName/Size, totales calculados, etc.
   - clientSuppliesPaper, isMissingSupply, missingSheetsQuantity,
     deliveredSheetsByClient, manualGoodSizes, manualSurplus se mantienen.

═══════════════════════════════════════════════════════════════════════════
2. ENDPOINTS VIGENTES (JWT en todos)
═══════════════════════════════════════════════════════════════════════════

Base: /api/v1/papers
Envelope éxito: { headers, timestamp, data }
Envelope error: { headers, timestamp, path, message, errors }

— Papeles —
POST   /api/v1/papers/register
PUT    /api/v1/papers/update/{paperId}
GET    /api/v1/papers/{paperId}
GET    /api/v1/papers
       Query: state?: boolean, coated?: boolean, page?, size?
       (paginado según PageResponse del proyecto)

Body create/update:
{
  "name": "Bond 75",          // required
  "grammage": 75.00,          // opcional
  "width": 70.00,             // required
  "height": 100.00,           // required
  "unit": "cm",               // cm|mm|in; default cm
  "coated": false,
  "state": true
}

Response Paper:
{
  "paperId", "companyId", "name", "grammage",
  "width", "height", "unit", "coated", "state",
  "creationDate", "updatedAt"
}

— Precios —
PUT  /api/v1/papers/{paperId}/prices     // REPLACE total de la lista
GET  /api/v1/papers/{paperId}/prices
GET  /api/v1/papers/{paperId}/prices/history

Body replace:
{
  "prices": [
    {
      "supplierId": "...",           // required
      "sheetValue": 1500.00,         // required > 0
      "packageUnit": 500,            // required > 0 (pliegos/empaque)
      "freightPerSheet": 0,          // >= 0; default 0
      "minPurchaseSheets": 500,      // opcional
      "paymentDays": 30,
      "deliveryDays": 5,
      "priceDate": "2026-10-06",
      "preferred": true,
      "state": true
    }
  ]
}

Response price (vigente):
{
  "paperSupplierPriceId", "supplierId", "sheetValue", "packageUnit",
  "freightPerSheet", "minPurchaseSheets", "paymentDays", "deliveryDays",
  "priceDate", "preferred", "state", "landedCostPerSheet"
}

History (solo GET): incluye packageUnit, effectiveFrom, changedBy; sin landedCost.

Catálogo despieces (/api/v1/cut-layouts):
  - Campos: name, width, height, unit, piecesPerSheet, cutValue?, state.
  - cutValue = precio/tarifa de corte por pliego (opcional, >= 0).

— Despieces por papel —
POST   /api/v1/papers/{paperId}/cut-layouts/register
PUT    /api/v1/papers/{paperId}/cut-layouts/update/{paperCutLayoutId}
GET    /api/v1/papers/{paperId}/cut-layouts/{paperCutLayoutId}
GET    /api/v1/papers/{paperId}/cut-layouts
DELETE /api/v1/papers/{paperId}/cut-layouts/{paperCutLayoutId}

Body create:
{
  "cutLayoutId": "...",
  "orientation": "vertical",   // vertical|horizontal|null
  "wastePercentage": 2.00,     // 0..100; SUGERENCIA UI
  "note": null,
  "state": true
}

Body update: orientation, wastePercentage, note, state
  (cutLayoutId no se cambia en update; para cambiar despiece: borrar + crear)

Response:
{
  "paperCutLayoutId", "paperId", "cutLayoutId",
  "orientation", "wastePercentage", "note", "state"
}

Catálogo de despieces (sigue igual):
GET/POST/PUT /api/v1/cut-layouts…

— Inventario —
POST   /api/v1/papers/{paperId}/stock/register
PUT    /api/v1/papers/{paperId}/stock/update/{paperStockId}
GET    /api/v1/papers/{paperId}/stock/{paperStockId}
GET    /api/v1/papers/{paperId}/stock?state=
DELETE /api/v1/papers/{paperId}/stock/{paperStockId}

Body create/update:
{
  "quantityInitial": 1000,
  "quantityAvailable": 1000,
  "unitCost": 1500.00,
  "entryDate": "2026-10-06",
  "state": true
}

— Remanentes reutilizables —
POST   /api/v1/papers/{paperId}/remnants/register
PUT    /api/v1/papers/{paperId}/remnants/update/{paperRemnantId}
GET    /api/v1/papers/{paperId}/remnants/{paperRemnantId}
GET    /api/v1/papers/{paperId}/remnants?state=
DELETE /api/v1/papers/{paperId}/remnants/{paperRemnantId}

Body create:
{
  "width": 35.00,
  "height": 50.00,
  "unit": "cm",
  "quantityInitial": 12.00,
  "quantityAvailable": 12.00,
  "unitCost": 0.00,
  "sourceProductionOrderId": null,
  "sourcePaperRowId": null,
  "entryDate": "2026-10-06",
  "note": "Sobrante de corte",
  "state": true
}

— OP Corte (actualizar paper rows) —
PUT /api/v1/production-orders/{productionOrderId}/paper-cutting
  (ruta exacta según controller vigente; revisar Swagger operation
   updateProductionOrderPaperCutting)

PaperRowRequest (fragmento relevante):
{
  "paperRowId": null,                 // o productionOrderPaperRowId (alias)
  "plateId": "...",                   // required
  "parentRowId": null,
  "cutRowKey": "cut-1",               // required
  "isMissingSupply": false,
  "missingSheetsQuantity": null,
  "clientSuppliesPaper": false,       // required
  "paperId": "paper-seed-001",        // ← NUEVO (reemplaza paperSheetId)
  "supplierId": "supplier-seed-001",
  "cutLayoutId": "cut-layout-seed-001",
  "paperCutLayoutId": "paper-cut-layout-001", // opcional; resuelve cutLayoutId
  "priceRule": "PREFERRED",           // PREFERRED|REPLACEMENT|BEST_COST
  "isPaperCut": true,
  "deliveredSheetsByClient": null,
  "manualGoodSizes": null,
  "manualSurplus": null,
  "plannedWastePercentage": 2.00      // merma OP; default compañía si omitido
}

También: machineUsages[], plannedMakereadyQuantity (arranque fijo; se SUMA
al %; no lo reemplaza). Ver módulo mermas/máquinas existente.

Mermas de compañía (para defaults / sugerencias de inputs OP):
GET/PUT /api/v1/waste-settings  (fase corte / cut)

═══════════════════════════════════════════════════════════════════════════
3. UX / PANTALLAS ESPERADAS
═══════════════════════════════════════════════════════════════════════════

1) Menú catálogo “Papeles” (único)
   - Listado: nombre, gramaje, formato (width×height unit), coated, state.
   - Filtros: state, coated.
   - Detalle / edición con tabs o secciones:
     a) Datos del papel (formato incluido)
     b) Precios por proveedor (replace list; marcar preferred; mostrar
        landedCostPerSheet read-only)
     c) Historial de precios (tabla read-only)
     d) Despieces asociados (CRUD; etiqueta clara:
        “% desperdicio sugerido (solo UI)”)
     e) Inventario / lotes (CRUD)

2) Eliminar pantallas y navegación de:
   - Tipos de papel
   - Pliegos / paper sheets
   - Cotizador paper-cost / quotes

3) Wizard OP — Corte de papel
   - Selector de papel desde GET /papers (mostrar formato + coated).
   - Al elegir papel: cargar precios → sugerir preferred o aplicar priceRule.
   - Al elegir despiece: preferir lista GET /papers/{id}/cut-layouts;
     permitir paperCutLayoutId O cutLayoutId.
   - Input merma de corte:
     * Default inicial: waste-settings de compañía (fase corte).
     * Opcional UX: si el paper_cut_layout tiene wastePercentage, ofrecerlo
       como sugerencia secundaria (chip/hint), NUNCA forzarlo en silencio
       como valor de cálculo sin que el usuario lo vea en plannedWastePercentage.
   - No mostrar cotizador paper-cost aparte; el costo lo calcula el backend
     al guardar el paso (snapshots en paperRows de la respuesta).

4) Tipos TypeScript / API layer
   - Renombrar PaperType* / PaperSheet* → Paper*
   - paperId en OP rows
   - Quitar paperSheetId, paperTypeId
   - packageUnit en Price (obligatorio) y snapshot OP
   - cutValue en CutLayout y snapshot OP
   - coated en Paper, no en Price

5) Copy / labels (ES)
   - “Papel” (no “tipo de papel” ni “pliego” como entidad de catálogo)
   - “Formato” = width × height + unit
   - “Esmaltado” = coated
   - “Costo landed” = sheetValue + flete (solo lectura)
   - “% sugerido de desperdicio (UI)” vs “Merma de corte (OP)”

═══════════════════════════════════════════════════════════════════════════
4. REGLAS DE IMPLEMENTACIÓN
═══════════════════════════════════════════════════════════════════════════

- No recrear endpoints eliminados ni “compat layer” hacia paper-types/sheets
  salvo un redirect de rutas viejas → /papers (opcional, una sola vez).
- No calcular merma de OP en frontend con wastePercentage del despiece; el
  backend es la fuente de verdad al guardar.
- No enviar Base64 ni campos inventados.
- Mantener envelope ApiSuccessEnvelope / manejo de errors[] existente.
- companyId sale del JWT/contexto; no lo pidas en forms de papers.
- Mostrar packageUnit en precios y cutValue en despieces; en OP snapshot
  si cutValue viene null, muestra “—” (tarifa no configurada).
- Actualizar mocks/tests e2e/storybook que aún usen paperSheetId o paper-types.
- Al terminar: checklist de archivos tocados + rutas eliminadas + rutas nuevas.

═══════════════════════════════════════════════════════════════════════════
5. CHECKLIST DE ACEPTACIÓN
═══════════════════════════════════════════════════════════════════════════

[ ] No quedan llamadas a /paper-types, /paper-sheets, /paper-cost
[ ] CRUD Papeles con width/height/unit/coated
[ ] Precios replace + list + history con packageUnit; preferred único; landedCost read-only
[ ] Catálogo despieces con cutValue; cut-layouts por papel con wastePercentage sugerencia UI
[ ] Stock CRUD por papel
[ ] OP corte envía paperId (+ paperCutLayoutId o cutLayoutId) y
    plannedWastePercentage; sin paperSheetId
[ ] Labels distinguen sugerencia UI vs merma OP
[ ] Types y menú alineados a “Papeles” único
```

---

## Notas rápidas (para humanos, no van en el prompt)

| Antes | Ahora |
|-------|--------|
| paper_types + paper_sheets | `papers` (material + formato) |
| precios en sheet/type | `paper_supplier_prices` bajo `/papers/{id}/prices` |
| cotizador `/paper-cost` | eliminado; costo en OP al guardar corte |
| `paperSheetId` en OP | `paperId` |
| `waste%` en vínculo despiece | solo UI; cálculo = `company_waste_settings` + `plannedWastePercentage` |
| `packageUnit` | en `paper_supplier_prices` (+ snapshot OP) |
| `cutValue` | en `cut_layouts` (+ snapshot OP) |
| OpenAPI | 0.0.68+ tag **Papeles** / **Despieces** |
