# Estructura del backend IndiCore (hexagonal)

Backend alineado con `docs/ARQUITECTURA_Y_CONSTRUCCION.md` y `docs/RESUMEN_PROYECTO.md` (referencia RFID Inventory).

## Paquete raÃ­z

`com.indicore`

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

| Archivo | Contenido |
|---------|-----------|
| `V1__indicolors_schema.sql` | Schema `indicolors` + extensiÃ³n `pgcrypto` |
| `V2__roles.sql` | Tabla `roles` + semilla administrador |
| `V3__users.sql` | Tabla `users` (columnas alineadas al script SQL) + semilla admin |
| `V4__clients.sql` | Tabla `clients` + semilla demo |

> Si ya tenÃ­as historial Flyway con migraciones antiguas (`indicore`), limpia la BD o haz baseline/reseteo del historial antes de aplicar esta serie.

## API y Swagger

- Convención de rutas y respuestas: `docs/CONVENCION_ENDPOINTS.md`
- Context path: `/indicore`
- Swagger UI: `/indicore/swagger-ui.html`
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
