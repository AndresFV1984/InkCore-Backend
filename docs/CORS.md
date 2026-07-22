# CORS (InkCore-backend)

## Qué hace

Whitelist de orígenes para que el frontend (Vite en `http://localhost:5173`) pueda llamar a
`http://localhost:8091/InkCore-backend` sin proxy. Preflight `OPTIONS` responde sin JWT.

**No** se usa `Access-Control-Allow-Origin: *`.

## Configuración

Prefijo: `app.cors`

| Propiedad | Descripción |
|-----------|-------------|
| `allowed-origins` | Lista YAML de orígenes |
| `origins` | CSV alternativo (`CORS_ALLOWED_ORIGINS`) si la lista está vacía |
| `allowed-methods` | Default: GET, POST, PUT, PATCH, DELETE, OPTIONS |
| `allowed-headers` | Authorization, Content-Type, Accept, Origin, X-Correlation-Id |
| `exposed-headers` | X-Correlation-Id (tokens van en el body del envelope) |
| `allow-credentials` | `false` (JWT en `Authorization`, sin cookies) |
| `max-age-seconds` | Cache del preflight (default 3600) |

### Desarrollo (`application-dev.yaml`)

```yaml
app:
  cors:
    allowed-origins:
      - http://localhost:5173
      - http://127.0.0.1:5173
```

El base path del front (`/inkcore/`) **no** forma parte del `Origin`.

### Staging / producción

```bash
# Windows PowerShell
$env:CORS_ALLOWED_ORIGINS="https://app.ejemplo.com,https://admin.ejemplo.com"

# Linux / macOS
export CORS_ALLOWED_ORIGINS="https://app.ejemplo.com,https://admin.ejemplo.com"
```

O lista en `application-prod.yaml` / `application-local.yaml` (ver `application-local.yaml.example`).

### Credenciales (cookies)

Hoy: `allow-credentials: false` (Bearer JWT).

Si en el futuro usáis cookies cross-origin: `allow-credentials: true` **y** orígenes concretos (nunca `*`).

## Verificación rápida

```bash
curl -i -X OPTIONS "http://localhost:8091/InkCore-backend/api/v1/roles/list" \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: authorization,content-type"
```

Esperado: `200`/`204` y `Access-Control-Allow-Origin: http://localhost:5173`.
