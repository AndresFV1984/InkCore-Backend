# Prompt frontend: proceso backend — Convertir IMG y Estimar tintas

Documento de referencia para el frontend. Describe **solo lo que hace el backend** en conversión de color y estimación de tintas tras la migración a LittleCMS 2.

Auth: Bearer JWT. Roles: `ADMINISTRADOR` | `OPERADOR`.  
Envelope: `{ headers, timestamp, data }` / errores `{ headers, timestamp, path, message, errors }`.

---

## Motor de color (compartido)

- CMM: **LittleCMS 2** (`lcms2`), nativa empaquetada en el JAR (Windows/Linux).
- No requiere instalación manual en el host ni configuración en el cliente.
- Perfiles ICC de destino: classpath `color-profiles/` (catálogo vía `GET /api/v1/color-conversions/list`, `available=true`).
- Misma pila LittleCMS + ICC para:
  - separación RGB → CMYK (CTP / convert),
  - soft-proof CMYK → RGB (preview JPEG),
  - RGB → CMYK dentro de estimación de tintas (imágenes RGB / RGB en PDF).

---

## 1. Convertir imagen / PDF — `POST /api/v1/color-conversions/convert`

`multipart/form-data` → envelope JSON. El archivo **no se guarda en disco**.

### Entrada (multipart)

| Campo | Obligatorio | Default / notas |
|---|---|---|
| `file` | sí | RGB: `.tif`, `.tiff`, `.jpg`, `.jpeg`, `.png`, `.pdf` |
| `renderingIntent` | no | `PERCEPTUAL` (fotos) o `RELATIVE_COLORIMETRIC` |
| `iccProfile` | no | `FOGRA39.icc` (o del catálogo / alias) |
| `outputFormat` | no | `TIFF` o `PDF` (según entrada si se omite) |
| `brightnessLift` | no | `0`–`0.20` (comercial típico `0.08`) |
| `vibranceBoost` | no | `0`–`0.35` (comercial típico `0.22`) |
| `softProofBrightnessMatch` | no | recupera brillo/croma tras ICC |
| `qualityPreset` | no | `FIDELITY` / `COMMERCIAL` / `VIVID` (solo rellena lo no enviado) |
| `blackPointCompensation` | no | **default servidor `true`** (LittleCMS BPC) |

Prioridad: overrides explícitos (`brightnessLift`, `vibranceBoost`, `softProofBrightnessMatch`, `blackPointCompensation`) ganan sobre `qualityPreset`.

### Proceso backend (orden)

1. Valida archivo, MIME/extensión y tamaño.
2. (Opcional) Ajustes RGB previos: `brightnessLift` / `vibranceBoost` según request o preset.
3. **Separación RGB → CMYK con LittleCMS** + perfil ICC de destino + intent + **BPC** (`blackPointCompensation`).
4. Si `softProofBrightnessMatch=true`: soft-proof CMYK→RGB (LittleCMS) y recuperación de brillo/croma sobre el CMYK.
5. Embebe ICC / escribe salida:
   - `TIFF`: TIFF CMYK LZW + ICC,
   - `PDF`: PDF CMYK con ICCBased + OutputIntent.
6. Genera **preview soft-proof** JPEG (CMYK→RGB LittleCMS) cuando aplica (típicamente salidas imagen/TIFF; null en PDF).
7. Responde Base64: archivo CMYK + preview RGB. Si LittleCMS no carga o el perfil falla → **error HTTP claro** (sin fallback silencioso).

### Salida (`data`)

| Campo | Rol |
|---|---|
| `fileBase64` + `contentType` + `fileName` | Archivo CMYK para descarga / CTP (`image/tiff` o `application/pdf`) |
| `previewRgbBase64` + `previewContentType` | Soft-proof JPEG para UI (`image/jpeg`; puede ser null) |
| `renderingIntent`, `iccProfile` | Valores efectivos usados |
| `brightnessLift`, `vibranceBoost`, `softProofBrightnessMatch`, `qualityPreset` | Efectivos / solicitados |
| `widthPx`, `heightPx`, tamaños, `processingTimeMs` | Metadatos |

---

## 2. Estimar tintas — `POST /api/v1/ink-estimates/estimate`

Dos modos (mismo path, distinto `Content-Type`):

### A) Multipart — formulario **Estimar tintas** (recomendado standalone)

`multipart/form-data` → envelope JSON. El archivo **no se guarda** en object storage.

| Campo | Obligatorio | Notas |
|---|---|---|
| `file` | sí | `.jpg`, `.jpeg`, `.png`, `.tif`, `.tiff`, `.webp`, `.gif`, `.pdf` |
| `sheetCount` | sí | Pliegos del pedido |
| `widthCm` / `heightCm` | **recomendado** | Default 70×100 — enviar tamaño real del trabajo |
| `dpi` | no | Default 300; raster / validación |
| `gramsPerCm2` | no | Si se envía: factor uniforme; si no: factores por canal del servidor |
| `iccProfile` | no | Destino LittleCMS para RGB (default `FOGRA39.icc`) |
| `pages` | no | PDF 1-based, máx. 2 (ej. `"1,2"`); si se omite, todas |

`operationId`: `estimateInkConsumptionMultipart`

### B) JSON + objectKey — artes ya en MinIO (OP / presign)

`application/json` con `objectKey` devuelto por `POST /api/v1/ink-estimate-assets/uploads` (presign + PUT navegador).

| Campo | Obligatorio | Notas |
|---|---|---|
| `objectKey` | sí | Clave staging `tmp/company/.../ink-estimates/...` |
| `sheetCount` | sí | Pliegos |
| Resto | no | Igual que multipart |

`operationId`: `estimateInkConsumptionFromObjectKey`

Presign upload (`POST /api/v1/ink-estimate-assets/uploads`): requiere `entradaId`; `plateId` opcional en staging. Máx. `object-storage.max-asset-file-bytes` (default **512 MB**, alineado con multipart).

### Proceso backend

**PDF (PDFBox)**

- Vectores/texto DeviceCMYK, Separation y DeviceN → cobertura área × tint.
- Imágenes CMYK/spot nativas → raster crudo.
- RGB/Gray → **LittleCMS + ICC** (`iccProfile`).

**Raster (TwelveMonkeys)**

- TIFF CMYK nativo: canales directos.
- JPG/PNG/WEBP/GIF → **RGB → LittleCMS + ICC**.

**Consumo**

- `gramos = (cobertura%/100) × área_cm² × factor_canal × sheetCount`
- Multi-página PDF: cobertura/gramos = **suma** de páginas seleccionadas (no promedio).
- Spots: se listan si hay Separation/DeviceN reportables (aunque cobertura 0%); gramos spot solo si `coverageMeasured=true`.

**Motor de color señalizado**

- `colorEngine = "littlecms"` en respuestas OK (CMM empaquetado + BPC).
- Si falla nativa/perfiles → error HTTP (sin `fallback-naive`).

### Salida (`data`) — campos relevantes al motor

| Campo | Significado |
|---|---|
| `colorEngine` | `"littlecms"` (solo en OK) |
| `iccProfileUsed` | Perfil usado por LittleCMS en RGB→CMYK |
| `processInks` / `spotInks` | Coberturas y gramos C/M/Y/K y spots |
| `hasSpotColors`, `declaredSpotColorNames`, `spotInventoryVerified` | Inventario spot |
| `pagesAnalyzed` | Páginas PDF 1-based analizadas |
| Totales `*GramsPerSheet` / `*GramsOrder` | Consumo pliego / pedido |

---

## Resumen para el front

| Flujo | CMM | Si falla LittleCMS |
|---|---|---|
| Convert | LittleCMS obligatorio | Error HTTP (sin degradación silenciosa) |
| Estimate | LittleCMS obligatorio | Error HTTP (sin degradación) |

## Precisión RGB (solo servidor)

Defaults HIGH en `application.yaml` (`inkcore.ink-estimation`):

- `max-analysis-pixels`: `4000000` (`INK_MAX_ANALYSIS_PIXELS`)
- `rgb-image-max-edge`: `2048` (`INK_RGB_IMAGE_MAX_EDGE`)
- `black-point-compensation`: `true` (`INK_BLACK_POINT_COMPENSATION`)

No se envían desde el front. Guía DevOps: FAST≈1e6/512 · BALANCED≈2e6/1024 · HIGH≈4e6/2048.

**Campos que sí envía el front** para precisión: ver sección **2. Estimar tintas** arriba (`widthCm`, `heightCm`, `sheetCount`, `iccProfile`, `dpi`, `pages`; omitir `gramsPerCm2` sin calibración).

---

Preview UI de convert: solo `previewRgbBase64`. Descarga CTP: solo `fileBase64` CMYK.
