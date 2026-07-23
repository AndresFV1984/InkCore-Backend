# Estructura del backend InkCore (hexagonal)

Backend alineado con `docs/ARQUITECTURA_Y_CONSTRUCCION.md` y `docs/RESUMEN_PROYECTO.md` (referencia RFID Inventory).

## Paquete raÃ­z

`com.inkcore`

## Base de datos

- **Schema SQL:** `indicolors` (como en el script de aprovisionamiento).
- **Flyway:** un archivo de migraciÃ³n por tabla (Ãºnica fuente de verdad del DDL); semillas en el mismo archivo donde aplica.

## Capas

```
src/main/java/com/indicore/
â”œâ”€â”€ IndicoreBackendApplication.java
â”‚
â”œâ”€â”€ domain/
â”‚   â”œâ”€â”€ shared/exception/
â”‚   â”œâ”€â”€ client/          # directorio de clientes
â”‚   â”œâ”€â”€ user/            # usuarios (tabla indicolors.users)
â”‚   â”œâ”€â”€ order/
â”‚   â””â”€â”€ remission/
â”‚
â”œâ”€â”€ application/
â”‚   â”œâ”€â”€ shared/AccessTokenPort.java   # puerto: emisiÃ³n JWT (impl. en infra)
â”‚   â”œâ”€â”€ client/usecase/
â”‚   â”œâ”€â”€ user/usecase/                 # CreateUser, Login, List, GetById
â”‚   â”œâ”€â”€ order/usecase/
â”‚   â””â”€â”€ remission/usecase/
â”‚
â””â”€â”€ infrastructure/
    â”œâ”€â”€ config/            # OpenAPI (esquema Bearer JWT)
    â”œâ”€â”€ security/          # SecurityConfig, JwtTokenService, JwtDecoder + validaciÃ³n tv
    â”œâ”€â”€ in/rest/
    â”‚   â”œâ”€â”€ auth/          # POST /api/v1/auth/login
    â”‚   â”œâ”€â”€ users/
    â”‚   â”œâ”€â”€ clients/
    â”‚   â”œâ”€â”€ envelope/
    â”‚   â””â”€â”€ errors/
    â””â”€â”€ out/persistence/
        â”œâ”€â”€ user/          # UserEntity, RoleEntity, adapter, mapper
        â”œâ”€â”€ client/
        â”œâ”€â”€ order/         # InMemory (stub)
        â””â”€â”€ remission/     # InMemory (stub)
```

## Dominios

| Dominio   | Tabla(s) Flyway      | REST principal |
|-----------|----------------------|----------------|
| **user**  | `roles`, `users`     | `/api/v1/users`, `/api/v1/auth/login` |
| **client**| `clients`            | `/api/v1/clients` |

## Seguridad (JWT)

- **Login:** `POST /api/v1/auth/login` (pÃºblico). Respuesta: `accessToken`, `tokenType`, `expiresInSeconds`.
- **JWT:** HS256; claims `sub` (userId), `tv` (token_version en BD), `roles` (lista de cÃ³digos de rol).
- **ValidaciÃ³n:** Resource Server + comparaciÃ³n de `tv` con `indicolors.users.token_version`.
- **AutorizaciÃ³n:** `@PreAuthorize` â€” ejemplo: `hasRole('ADMINISTRADOR')` para alta/listado de usuarios.
- **Clientes:** `isAuthenticated()` + bearer en Swagger.
- **Dev rÃ¡pido:** `SECURITY_PERMIT_ALL=true` desactiva JWT en filtros (no usar en producciÃ³n).

Variables recomendadas: `JWT_SECRET` (â‰¥ 32 caracteres), `JWT_EXP_SECONDS`.

## Usuario semilla (despuÃ©s de migraciones)

- **Mail:** `admin@indicolors.com`
- **ContraseÃ±a:** `Indicore2026!`
- **Rol:** `ADMINISTRADOR` (`role_id` fijo en `V2__roles.sql`)

## Migraciones Flyway

Convención: **un archivo por tabla** (DDL + semilla mínima si aplica). Schema/extensión en `V1`.

| Archivo | Tabla / contenido |
|---------|-------------------|
| `V1__schema.sql` | Schema `indicolors` + extensión `pgcrypto` |
| `V2__companies.sql` | `companies` + semilla |
| `V3__roles.sql` | `roles` + semillas Administrador / Operador |
| `V4__permissions.sql` | `permissions` + catálogo |
| `V5__role_permissions.sql` | `role_permissions` (solo DDL) |
| `V6__users.sql` | `users` + admin semilla |
| `V7__user_roles.sql` | `user_roles` + vínculo admin |
| `V8__clients.sql` | `clients` |

Scripts informativos (fuera de Flyway, ejecutar a mano en BD):

| Archivo | Uso |
|---------|-----|
| `scripts/postgres/assign-role-permissions.sql` | Asignar / consultar / revocar permisos de negocio a roles |

Roles PostgreSQL (Script_Crear_BD):

| Rol | Uso |
|-----|-----|
| `indicolors_owner` | DDL / Flyway (único autorizado a migraciones) |
| `indicolors_app` | Runtime de la aplicación (DML) |
| `inkcore_admin` | Admin general de la BD (opcional) |

> No existe `indicolors_migrator`. Si ya tenías historial Flyway con migraciones antiguas, limpia la BD o haz baseline/reseteo del historial antes de aplicar esta serie.

## Docker (API + Postgres + Redis + Sonar)

Imagen: **`bayronindicore/inkcore-backend`** (Docker Hub). Stack alineado al patrón Rafex.

```powershell
# Publicar a Docker Hub (tag automático v + día.mes, ej. v22.07 + latest)
.\scripts\docker\push-hub.ps1

# Levantar stack
docker compose up -d --build
```

| Servicio | URL / puerto |
|----------|----------------|
| API | http://localhost:8091/InkCore-backend |
| Swagger | http://localhost:8091/InkCore-backend/swagger-ui.html |
| Postgres (`db`) | `localhost:15432` |
| Redis | `localhost:6379` |
| Redis Insight | http://localhost:8092 |
| SonarQube | http://localhost:9000 |

Datos persistentes en host: `C:/inkcore_postgres_data`, `C:/inkcore_redis_data`.

Versión de imagen: formato **`vdd.MM`** (ej. `v22.07`). Si cambias init/passwords: `docker compose down -v` y vuelve a levantar.

## API y Swagger

- Convención de rutas y respuestas: `docs/CONVENCION_ENDPOINTS.md`
- Context path: `/InkCore-backend`
- Swagger UI: `/InkCore-backend/swagger-ui.html`
- En endpoints protegidos, usar **Authorize** con el token devuelto por login.

## SOLID (resumen)

- **S:** Controller â‰  caso de uso â‰  mapper â‰  adapter.
- **O:** Nuevos adaptadores sin tocar el dominio.
- **L:** Implementaciones de puertos sustituibles.
- **I:** Puertos acotados (`UserRepositoryPort`, `AccessTokenPort`, â€¦).
- **D:** Casos de uso dependen de interfaces, no de JPA ni Nimbus.

## PrÃ³ximos pasos sugeridos

- Refresh token y rotaciÃ³n (como en `RESUMEN_PROYECTO.md`).
- PolÃ­tica de bloqueo por `failed_attempts` / `locked_until`.
- JPA real para `order` y `remission`.
