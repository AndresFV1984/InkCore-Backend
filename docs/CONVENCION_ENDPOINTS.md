# Convención de endpoints InkCore

## Regla

Toda ruta REST debe incluir **una palabra de acción** después del recurso:

```
{METHOD} /api/v1/{recurso}/{accion}[/parametros]
```

Context path de la aplicación: `/InkCore-backend` (configurable con `APP_CONTEXT`).

## Vocabulario permitido

| Acción | Uso | HTTP típico |
|--------|-----|-------------|
| `login` | Autenticación | POST |
| `refresh` | Renovar token (futuro) | POST |
| `register` | Alta de recurso | POST |
| `list` | Listado | GET |
| `get` | Detalle por ID | GET |
| `profile` | Recurso del usuario autenticado | GET |
| `update` | Modificación | PUT / PATCH |
| `delete` | Baja | DELETE |
| `search` | Búsqueda con query params | GET |
| `convert` | Transformación de archivo (p. ej. color RGB→CMYK) | POST |
| `estimate` | Cálculo / estimación (p. ej. consumo de tinta) | POST |

No usar sinónimos fuera de esta tabla (`create`, `fetch`, etc.).

## Endpoints actuales

| Método | Ruta | operationId | Descripción |
|--------|------|-------------|-------------|
| POST | `/api/v1/auth/login` | `login` | Iniciar sesión |
| POST | `/api/v1/users/register` | `registerUser` | Registrar usuario (ADMIN) |
| PUT | `/api/v1/users/update/{userId}` | `updateUser` | Actualizar usuario (ADMIN) |
| GET | `/api/v1/users/list` | `listUsers` | Listar usuarios (ADMIN) |
| GET | `/api/v1/users/profile` | `profileUser` | Perfil del token actual |
| GET | `/api/v1/users/get/{userId}` | `getUser` | Usuario por ID |
| POST | `/api/v1/clients/register` | `registerClient` | Registrar cliente (JWT; incluye `documentType`) |
| PUT | `/api/v1/clients/update/{clientId}` | `updateClient` | Actualizar cliente (JWT; incluye `documentType`) |
| GET | `/api/v1/clients/list` | `listClients` | Listar clientes (JWT) |
| GET | `/api/v1/clients/get/{clientId}` | `getClient` | Cliente por ID (JWT) |
| GET | `/api/v1/roles/list` | `listRoles` | Catálogo de roles (ADMIN) |
| GET | `/api/v1/permissions/list` | `listPermissions` | Catálogo de permisos (ADMIN) |
| POST | `/api/v1/color-conversions/convert` | `convertColorSpace` | Convertir RGB→CMYK (multipart; iccProfile catálogo FOGRA/GRACoL/SWOP; ajustes opcionales; JWT) |
| GET | `/api/v1/color-conversions/list` | `listColorConversionIccProfiles` | Listar perfiles ICC de destino (disponibilidad en servidor; JWT) |
| POST | `/api/v1/ink-estimates/estimate` | `estimateInkConsumption` | Estimar tinta CMYK+spot; `pages` opcional (máx. 2, 1-based); multipart; JWT |
| POST | `/api/v1/cut-layouts/register` | `registerCutLayout` | Registrar despiece (JWT) |
| PUT | `/api/v1/cut-layouts/update/{cutLayoutId}` | `updateCutLayout` | Actualizar despiece (JWT) |
| GET | `/api/v1/cut-layouts/list` | `listCutLayouts` | Listar despieces (JWT; filtros `companyId`, `state`) |
| GET | `/api/v1/cut-layouts/get/{cutLayoutId}` | `getCutLayout` | Despiece por ID (JWT) |
| POST | `/api/v1/papers/register` | `registerPaper` | Registrar papel (JWT; formato + acceptsRemnants/minRemnant W/H/unit) |
| PUT | `/api/v1/papers/update/{paperId}` | `updatePaper` | Actualizar papel (JWT; incluye política de remanentes) |
| GET | `/api/v1/papers/{paperId}` | `getPaper` | Papel por ID (JWT) |
| GET | `/api/v1/papers` | `listPapers` | Listar papeles (JWT; filtros `state`, `coated`, `acceptsRemnants`) |
| PUT | `/api/v1/papers/{paperId}/prices` | `replacePaperPrices` | Reemplazar precios del papel (JWT) |
| GET | `/api/v1/papers/{paperId}/prices` | `listPaperPrices` | Listar precios vigentes del papel (JWT) |
| GET | `/api/v1/papers/{paperId}/prices/history` | `listPaperPriceHistory` | Historial de precios del papel (JWT) |
| POST | `/api/v1/papers/{paperId}/cut-layouts/register` | `registerPaperCutLayout` | Asociar despiece al papel (JWT) |
| PUT | `/api/v1/papers/{paperId}/cut-layouts/update/{paperCutLayoutId}` | `updatePaperCutLayout` | Actualizar despiece por papel (JWT) |
| GET | `/api/v1/papers/{paperId}/cut-layouts/{paperCutLayoutId}` | `getPaperCutLayout` | Despiece por papel por ID (JWT) |
| GET | `/api/v1/papers/{paperId}/cut-layouts` | `listPaperCutLayouts` | Listar despieces del papel (JWT) |
| DELETE | `/api/v1/papers/{paperId}/cut-layouts/{paperCutLayoutId}` | `deletePaperCutLayout` | Eliminar despiece por papel (JWT) |
| POST | `/api/v1/papers/{paperId}/stock/register` | `registerPaperStock` | Registrar lote de inventario (JWT) |
| PUT | `/api/v1/papers/{paperId}/stock/update/{paperStockId}` | `updatePaperStock` | Actualizar lote de inventario (JWT) |
| GET | `/api/v1/papers/{paperId}/stock/{paperStockId}` | `getPaperStock` | Lote de inventario por ID (JWT) |
| GET | `/api/v1/papers/{paperId}/stock` | `listPaperStock` | Listar inventario del papel (JWT; filtro `state`) |
| DELETE | `/api/v1/papers/{paperId}/stock/{paperStockId}` | `deletePaperStock` | Eliminar lote de inventario (JWT) |
| POST | `/api/v1/papers/{paperId}/remnants/register` | `registerPaperRemnant` | Registrar remanente reutilizable de corte (JWT) |
| PUT | `/api/v1/papers/{paperId}/remnants/update/{paperRemnantId}` | `updatePaperRemnant` | Actualizar remanente (JWT) |
| GET | `/api/v1/papers/{paperId}/remnants/{paperRemnantId}` | `getPaperRemnant` | Remanente por ID (JWT) |
| GET | `/api/v1/papers/{paperId}/remnants` | `listPaperRemnants` | Listar remanentes del papel (JWT; filtro `state`) |
| DELETE | `/api/v1/papers/{paperId}/remnants/{paperRemnantId}` | `deletePaperRemnant` | Eliminar remanente (JWT) |

## Contrato de respuesta (Swagger)

- **Éxito (JSON):** `ApiSuccessEnvelope` → `headers`, `timestamp`, `data`
- **Éxito (conversión de color):** archivo binario (`image/tiff` o `application/pdf`) + cabeceras `X-*`
- **Error:** `ApiErrorEnvelope` → `headers`, `timestamp`, `path`, `message`, `errors`

## Checklist para nuevos endpoints

1. Elegir acción del vocabulario.
2. `@XxxMapping("/{accion}")` en el controlador.
3. `@Operation(operationId = "...", summary = "...")`.
4. Anotar errores con `@ApiErrorResponses` y, si requiere JWT, `@ApiSecuredErrorResponses`.
5. Actualizar la tabla de este documento.
6. Probar en Swagger UI: `/InkCore-backend/swagger-ui.html`
