# Convención de endpoints IndiCore

## Regla

Toda ruta REST debe incluir **una palabra de acción** después del recurso:

```
{METHOD} /api/v1/{recurso}/{accion}[/parametros]
```

Context path de la aplicación: `/indicore` (configurable con `APP_CONTEXT`).

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

No usar sinónimos fuera de esta tabla (`create`, `fetch`, etc.).

## Endpoints actuales

| Método | Ruta | operationId | Descripción |
|--------|------|-------------|-------------|
| POST | `/api/v1/auth/login` | `login` | Iniciar sesión |
| POST | `/api/v1/users/register` | `registerUser` | Registrar usuario (ADMIN) |
| GET | `/api/v1/users/list` | `listUsers` | Listar usuarios (ADMIN) |
| GET | `/api/v1/users/profile` | `profileUser` | Perfil del token actual |
| GET | `/api/v1/users/get/{userId}` | `getUser` | Usuario por ID |
| POST | `/api/v1/clients/register` | `registerClient` | Registrar cliente |
| GET | `/api/v1/clients/list` | `listClients` | Listar clientes |
| GET | `/api/v1/clients/get/{id}` | `getClient` | Cliente por UUID |

## Contrato de respuesta (Swagger)

- **Éxito:** `ApiSuccessEnvelope` → `headers`, `timestamp`, `data`
- **Error:** `ApiErrorEnvelope` → `headers`, `timestamp`, `path`, `message`, `errors`

## Checklist para nuevos endpoints

1. Elegir acción del vocabulario.
2. `@XxxMapping("/{accion}")` en el controlador.
3. `@Operation(operationId = "...", summary = "...")`.
4. Anotar errores con `@ApiErrorResponses` y, si requiere JWT, `@ApiSecuredErrorResponses`.
5. Actualizar la tabla de este documento.
6. Probar en Swagger UI: `/indicore/swagger-ui.html`
