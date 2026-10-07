# Prompt Frontend — Política de remanentes en papeles

> Copia el bloque de abajo y úsalo en el agente/chat del frontend InkCore.
> Backend OpenAPI **0.0.73+**. Complementa `Prompt_Frontend_Inventario_Papel.md`
> y el catálogo de papeles.

---

## PROMPT (copiar desde aquí)

```
Trabajas en el frontend de InkCore/Indicolors. El backend agregó política de
remanentes en el catálogo papers y el CRUD de paper_remnants. Debes adaptar
formularios de papel, selector de remanentes y validaciones de UI.
NO inventes endpoints ni campos.

═══════════════════════════════════════════════════════════════════════════
1. CAMBIOS EN EL CATÁLOGO papers (configuración)
═══════════════════════════════════════════════════════════════════════════

Campos nuevos en create/update/get/list de papeles:

{
  "acceptsRemnants": true,       // boolean; default false
  "minRemnantWidth": 20.00,      // obligatorio EN EL PAPEL si acceptsRemnants=true
  "minRemnantHeight": 20.00,     // idem
  "minRemnantUnit": "cm"         // unidad PROPIA de las mínimas (cm|mm|in);
                                 // si se omite y accepts=true, backend usa unit del pliego
}

Nota: unit del papel = pliego de compra. minRemnantUnit = unidad de las mínimas
de remanente (se guarda aparte; puede diferir del pliego, p.ej. pliego en cm y
mínimas en mm).

Reglas al GUARDAR el papel (backend sí valida esto):
- acceptsRemnants=false → minRemnantWidth, minRemnantHeight y minRemnantUnit
  deben ser null.
- acceptsRemnants=true → width/height/unit mínimas obligatorias (unit default=
  unit del pliego si se omite), > 0, y no superar el pliego (comparación
  convirtiendo a mm).

Listado con filtro:
GET /api/v1/papers?state=true&acceptsRemnants=true

UI catálogo / formulario papel:
- Switch “Acepta remanentes reutilizables”.
- Si está ON: mostrar minRemnantWidth, minRemnantHeight y minRemnantUnit
  (select cm|mm|in; default = unit del pliego).
- Si está OFF: ocultar/limpiar mínimas+unidad y enviar null.
- Detalle/listado: “Acepta remanentes” + “Mín. W×H minRemnantUnit”.

═══════════════════════════════════════════════════════════════════════════
2. REGISTRO DE REMANENTES — VALIDACIÓN SOLO EN FRONTEND
═══════════════════════════════════════════════════════════════════════════

Endpoints (sin cambio de contrato de body):
POST   /api/v1/papers/{paperId}/remnants/register
PUT    /api/v1/papers/{paperId}/remnants/update/{paperRemnantId}
GET    /api/v1/papers/{paperId}/remnants?state=
GET    /api/v1/papers/{paperId}/remnants/{paperRemnantId}
DELETE /api/v1/papers/{paperId}/remnants/{paperRemnantId}

DECISIÓN CRÍTICA DE PRODUCTO:
- El BACKEND al crear/actualizar remanente SOLO exige que el paperId exista.
- NO bloquea si acceptsRemnants=false.
- NO bloquea si width/height quedan por debajo de minRemnant*.
- Motivo: en planta hay casos especiales con sobrantes más pequeños que el
  mínimo configurado; deben poder registrarse igual.

Por tanto la política del papel es GUÍA DE UI, no hard-block del API.

Comportamiento UI obligatorio:

1) Selector de papel al crear remanente
   - Preferir: GET /api/v1/papers?state=true&acceptsRemnants=true
   - Opcional: permitir “mostrar todos los papeles” para excepciones.
   - Si eligen un papel con acceptsRemnants=false → warning + confirmar
     (“Este papel no está marcado para remanentes. ¿Registrar de todos modos?”).

2) Medidas del remanente (width, height, unit)
   - Prefijar unit del remanente = paper.minRemnantUnit ?? paper.unit.
   - Mostrar referencia:
     “Mínimo sugerido: {minRemnantWidth} × {minRemnantHeight} {minRemnantUnit}”
     y “Pliego de compra: {width} × {height} {unit}”.
   - Soft-validate (avisar, no impedir sin confirmación):
     a) Si medidas < mínimas (permitir rotar W×H al comparar):
        warning + confirmación “Medida por debajo del mínimo configurado
        (caso especial). ¿Continuar?”
     b) Si medidas > formato de compra (también con rotación):
        warning fuerte + confirmación.
   - Tras confirmar, enviar el POST/PUT normal; el backend lo aceptará.

3) No inventar campos en remanentes
   - El remanente sigue con sus width/height/unit propios.
   - No guardar acceptsRemnants en paper_remnants.

4) Stock (paper_stock) no usa esta política
   - Inventario de pliegos es independiente; no mezclar con remanentes.

═══════════════════════════════════════════════════════════════════════════
3. TIPOS (fragmento)
═══════════════════════════════════════════════════════════════════════════

// en Paper
acceptsRemnants: boolean;
minRemnantWidth: number | null;
minRemnantHeight: number | null;
minRemnantUnit: "cm" | "mm" | "in" | null;

// PaperRemnant sin cambios de política
type PaperRemnant = {
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

═══════════════════════════════════════════════════════════════════════════
4. CHECKLIST
═══════════════════════════════════════════════════════════════════════════

[ ] Formulario papel: acceptsRemnants + minRemnantWidth/Height/Unit
[ ] List/detail papel muestran política de remanentes
[ ] Filtro acceptsRemnants en selector para dashboard remanentes
[ ] Soft-warnings + confirmación para excepciones (< mínimo o papel no marcado)
[ ] NUNCA asumir que el API rechazará medidas bajo el mínimo
[ ] Separar pantallas stock vs remanentes
```

---

## Dónde usarlo

1. Prompt corto (este archivo): cambios de política + validación UI.
2. Prompt completo de inventarios: `Prompt_Frontend_Inventario_Papel.md`
   (dashboards stock + remanentes).
3. Catálogo general: `Prompt_Frontend_Catalogo_Papeles.md` (ya menciona la política).
