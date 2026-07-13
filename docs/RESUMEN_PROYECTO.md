# Resumen del proyecto (RFID Inventory Backend)

Este documento ofrece un resumen **corto y práctico** del backend `rfid-inventory`: qué hace, cómo está construido, cómo funciona la autenticación (access token + refresh token) y qué conceptos de arquitectura/patrones/SOLID conviene conocer para contribuir.

> Para documentación en profundidad, ver el índice: `docs/INDICE_DOCUMENTACION.md`.

---

## 1) ¿Qué es este proyecto?

Backend REST para apoyar procesos de inventario con RFID (bodegas, lectores RFID, usuarios/roles/permisos, inventarios y sus estados). Está construido con **Spring Boot**, persistencia en **PostgreSQL** y migraciones con **Flyway**.

**Tecnologías clave**
- Java + Spring Boot
- Spring Web (REST)
- Spring Security (resource server JWT)
- PostgreSQL + JPA/Hibernate
- Flyway (migraciones)
- Redis opcional (refresh tokens / cache), con fallback **in-memory** en dev
- OpenAPI/Swagger (springdoc)

---

## 2) Cómo se organiza el código (arquitectura hexagonal)

La estructura principal sigue una arquitectura **Hexagonal (Ports & Adapters)** con **7 dominios de negocio**:

- `com.itm.rfid_inventory.domain`
  - **user/** — Gestión de usuarios, roles y permisos
  - **company/** — Gestión de empresas/organizaciones
  - **warehouse/** — Gestión de almacenes/bodegas y tipos
  - **rfidmodel/** — Gestión de modelos de lectores RFID
  - **rfidreader/** — Gestión de lectores RFID físicos
  - **inventory/** — Gestión de inventarios y sus estados
  - **inventorydetail/** — Gestión de detalles/items en un inventario

- `com.itm.rfid_inventory.application`
  - Casos de uso para cada dominio (use cases) y puertos (interfaces) hacia el exterior.

- `com.itm.rfid_inventory.infrastructure`
  - Adaptadores de entrada (REST con controllers y DTOs)
  - Adaptadores de salida (persistencia JPA, mappers)
  - Servicios de seguridad (JWT, tokens)

**Idea central:**
- El **dominio** y los **casos de uso** no dependen de frameworks.
- Los frameworks (Spring, JPA, Redis) viven en **infraestructura**.
- La comunicación se hace por **interfaces (ports)**.

Documentos recomendados:
- `docs/ARQUITECTURA_Y_CONSTRUCCION.md`
- `docs/architecture-hexagonal.md`

---

## 3) Autenticación y tokens (Access Token + Refresh Token)

### 3.1 Access Token (JWT)
- Se usa `Authorization: Bearer <token>`.
- El JWT incluye claims mínimos como:
  - `sub` (subject: normalmente el identificador del usuario)
  - `iat` (issued at)
  - `exp` (expires)
  - `roles` (lista)
  - `permissions` (lista)
  - `tv` (**token version**)

El backend valida:
1) Firma y expiración del JWT (HS256)
2) Que `tv` (claim del token) coincida con la versión vigente del usuario

> Esto permite invalidar tokens antiguos sin necesidad de blacklist por token.

#### 3.1.1 ¿Dónde se genera el access token?

El access token se genera **solo** en el flujo de login:

- Endpoint: `POST /api/v1/users/login`
- Caso de uso: `LoginUserUseCase`
- Servicio: `JwtTokenService`

Referencia en código:
- `src/main/java/com/itm/rfid_inventory/application/user/usecase/LoginUserUseCase.java`:
  - Generación del JWT: `jwtTokenService.generateToken(user.userId(), user.tokenVersion(), rolesForToken, permissionsForToken)`.
- `src/main/java/com/itm/rfid_inventory/infrastructure/security/JwtTokenService.java`:
  - Método: `generateToken(subject, tokenVersion, roles, permissions)`.

#### 3.1.2 Estructura del JWT (qué contiene)

Un JWT tiene 3 partes:

`base64url(header) . base64url(payload) . base64url(signature)`

En este proyecto el header es fijo (HS256):

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

El payload (claims) tiene esta forma (ejemplo simplificado):

```json
{
  "sub": "<userId>",
  "iat": 1715100000,
  "exp": 1715103600,
  "tv": 1,
  "roles": ["ADMINISTRADOR"],
  "permissions": ["USUARIO_VER", "USUARIO_CREAR"]
}
```

La firma se calcula con HMAC-SHA256 usando el secreto configurado por:

- Propiedad: `security.jwt.secret`
- o variable de entorno: `JWT_SECRET`

> Nota: el secreto debe tener **>= 32 bytes** para HS256.

#### 3.1.3 Explicación campo por campo (claims)

1) **`sub` (subject)**
- Qué es: identificador del “dueño” del token.
- En este backend: se usa el **`userId`** como subject (ver `LoginUserUseCase`).
- Uso: con `sub` se obtiene el usuario actual en endpoints protegidos.

2) **`iat` (issued at)**
- Qué es: fecha/hora de emisión del token.
- Formato: *epoch seconds* (segundos desde 1970-01-01 UTC).
- Uso típico: auditoría/trazabilidad.

3) **`exp` (expires at)**
- Qué es: fecha/hora de expiración.
- Formato: *epoch seconds*.
- Cómo se calcula: `now + security.jwt.expiration-seconds`.
- Configuración:
  - `security.jwt.expiration-seconds` (o `JWT_EXP_SECONDS`)
  - Por defecto suele ser `3600` (1 hora) (ver `BeansConfig`).

4) **`tv` (token version)**
- Qué es: versión vigente de sesión para el usuario.
- Para qué sirve: invalidación global de tokens antiguos. Si el usuario cambia contraseña o se ejecuta un “logout all”, se incrementa la versión y los tokens anteriores quedan inválidos.
- Cómo se valida:
  - En cada request, Spring Security decodifica el JWT y luego se compara `tv` (claim) contra el valor vigente consultado en `UserTokenVersionService`.
  - Si no coincide, se rechaza con token inválido.

5) **`roles`**
- Qué es: lista de roles del usuario.
- Uso: se convierten a authorities con prefijo `ROLE_`.
  - Ejemplo: `ADMINISTRADOR` → `ROLE_ADMINISTRADOR`
- Se usan con anotaciones como `@PreAuthorize("hasRole('ADMINISTRADOR')")`.

6) **`permissions`**
- Qué es: lista de permisos finos del usuario.
- Uso: se convierten a authorities con prefijo `PERMISSION_`.
  - Ejemplo: `USUARIO_CREAR` → `PERMISSION_USUARIO_CREAR`
- Se usan con anotaciones como `@PreAuthorize("hasAuthority('PERMISSION_USUARIO_CREAR')")`.

#### 3.1.4 ¿Cómo se valida el JWT en los endpoints?

En `SecurityConfig` se aplica este pipeline:

1) **Validación de firma y expiración** (`exp`) usando `NimbusJwtDecoder` (HS256).
2) **Validación de `tv`** comparando el claim `tv` contra `UserTokenVersionService.getCurrentTokenVersion(jwt.getSubject())`.
3) **Conversión de roles/permisos a authorities** con `JwtAuthoritiesConverter`.
4) Autorización fina a nivel de método con `@PreAuthorize`.

### 3.2 Refresh Token (opaco y stateful)
- El refresh token **NO es JWT**. Es un string aleatorio.
- Se guarda en servidor (store stateful) con:
  - `userId`
  - `tokenVersion`
  - `expiresAt`

El refresh token se usa para emitir un nuevo access token cuando el access token expira.

### 3.3 Rotación del refresh token
Configurable con:
- `security.refresh-token.rotate-on-refresh` (por defecto `true` en dev)

Si está en `true`, en cada refresh se emite un refresh token nuevo y se invalida el anterior.

### 3.4 Almacenamiento del refresh token (Redis o memoria)
Configurable con:
- `security.refresh-token.store.type` (`redis` | `memory`)

En `application-dev.yaml` el default es `memory` para facilitar desarrollo local.

### 3.5 Token Version (`tv`) e invalidación de sesiones
La **token version** vive asociada al usuario.

Cuando ocurre un evento de seguridad, se incrementa `tokenVersion` y automáticamente:
- todos los access tokens emitidos antes quedan inválidos (porque su `tv` ya no coincide)
- los refresh tokens asociados a la versión anterior también pueden invalidarse

Eventos típicos:
- cambio de contraseña
- “logout all” (invalidar todas las sesiones)

Documentación recomendada:
- `docs/ENDPOINTS_TOKENS.md`

---

## 4) Endpoints que emiten tokens (regla de oro)

**Regla:** solo estos endpoints devuelven tokens:
- `POST /api/v1/users/login`
- `POST /api/v1/auth/refresh`

Todos los demás endpoints retornan datos “limpios” (sin refresh token ni expiraciones), para evitar fugas y acoplamientos.

> Detalle y ejemplos: `docs/ENDPOINTS_TOKENS.md`.

---

## 5) Autorización (roles y permisos)

El JWT trae:
- `roles` → se convierten a authorities `ROLE_<ROL>`
- `permissions` → se convierten a authorities `PERMISSION_<PERMISO>`

Esto permite proteger endpoints por:
- rol (p.ej. ADMINISTRADOR)
- permisos finos (p.ej. PERMISSION_INVENTARIO_CREAR)

### 5.1 ¿Cómo se valida en un endpoint si el usuario puede acceder al recurso?

En este backend la decisión se toma en **dos capas**:

1) **Autenticación (¿quién eres?)** → si falla: **401 Unauthorized**
2) **Autorización (¿tienes permiso?)** → si falla: **403 Forbidden**

#### Capa 1: autenticación (JWT válido)

Está definida en `src/main/java/com/itm/rfid_inventory/infrastructure/security/SecurityConfig.java`:

- Por defecto: `.anyRequest().authenticated()` (casi todo requiere token)
- Excepciones públicas: login, refresh y Swagger (se permiten sin token)
- Validaciones aplicadas:
  - **firma HS256** y **expiración** (`exp`) con `NimbusJwtDecoder`
  - validación adicional de **tokenVersion** comparando `tv` vs valor vigente en servidor (`UserTokenVersionService`)

Si el token es inválido/expiró/no coincide `tv`, Spring Security **no deja entrar** al controller y responde 401.

#### Capa 2: autorización (roles/permisos)

La autorización está implementada con **seguridad a nivel de método**:

- `@EnableMethodSecurity` (en `SecurityConfig`) habilita `@PreAuthorize`.
- Cada controller marca qué necesita para ejecutar el método.

El JWT trae listas `roles` y `permissions`, y se convierten a *authorities* así:

- `roles: ["ADMINISTRADOR"]` → authorities: `ROLE_ADMINISTRADOR`
- `permissions: ["USUARIO_VER"]` → authorities: `PERMISSION_USUARIO_VER`

(ver `src/main/java/com/itm/rfid_inventory/infrastructure/security/JwtAuthoritiesConverter.java`).

Luego, Spring evalúa el `@PreAuthorize(...)` antes de ejecutar el método:

- Si **cumple** la expresión → entra al endpoint.
- Si **no cumple** → responde **403 Forbidden**.

#### Ejemplos reales de la regla “¿puedo acceder?”

- Listar bodegas requiere permiso de ver bodegas o rol admin:
  - `WarehouseController.list()`
  - `@PreAuthorize("hasAuthority('PERMISSION_BODEGA_VER') or hasRole('ADMINISTRADOR')")`

- Crear usuario requiere permiso de crear usuarios o rol admin:
  - `UserController.create()`
  - `@PreAuthorize("hasAuthority('PERMISSION_USUARIO_CREAR') or hasRole('ADMINISTRADOR')")`

- Operaciones de seguridad solo para admin:
  - `SecurityTokenController.tokenInfo()` y `logoutAll()`
  - `@PreAuthorize("hasRole('ADMINISTRADOR')")`

#### (Opcional) Validación de “propiedad” del recurso

Cuando se necesita una regla del tipo “solo el dueño puede editar/ver X”, hay dos formas comunes:

1) **En el caso de uso (regla de negocio)**: validar que el recurso pertenezca a la empresa/usuario antes de operar.
2) **En `@PreAuthorize` con SpEL** (si existe un servicio de autorización), por ejemplo:
   - `@PreAuthorize("@authz.canReadInventory(authentication, #inventoryId)")`

En el código ya se ve el patrón de obtener el usuario actual desde el token:
- `Jwt jwt = (Jwt) authentication.getPrincipal();`
- `String userId = jwt.getSubject();`

Eso permite aplicar reglas por usuario cuando el negocio lo requiera.

---

## 6) Seguridad de cuenta (bloqueo, expiración, cambio forzado)

El modelo de usuario contiene campos de seguridad como:
- `failedAttempts`
- `lockedUntil`
- `passwordExpiresAt`
- `forcePasswordChange`
- `passwordChangedAt`
- `lastLoginAt`

Parámetros configurables por ambiente (ver `application-*.yaml`):
```yaml
security:
  password:
    max-failed-attempts: ${PASSWORD_MAX_FAILED_ATTEMPTS:5}
    lock-duration-minutes: ${PASSWORD_LOCK_DURATION_MINUTES:15}
    expiration-days: ${PASSWORD_EXPIRATION_DAYS:90}
    warning-days: ${PASSWORD_WARNING_DAYS:7}
```

Documento recomendado:
- `docs/LOGICA_LOGIN_CAMBIO_CONTRASENA.md`

---

## 7) Manejo de errores y contrato de respuesta

El backend responde consistentemente con un **sobre (envelope)** tipo `ApiEnvelope`:
- `headers` (metadata)
- `timestamp`
- `path`
- `message`
- `errors` (cuando aplica)
- `data`

Notas importantes:
- Los campos relacionados con tokens se omiten si son `null`.
- Las validaciones (Bean Validation) se devuelven con mensajes legibles.

Documento recomendado:
- `docs/MEJORA_MENSAJES_ERROR.md`

---

## 8) Persistencia y migraciones

- Las tablas viven en el schema: `rfid_inventory`.
- Las migraciones Flyway están en: `src/main/resources/db/migration`.
- En dev normalmente:
  - `spring.jpa.hibernate.ddl-auto=validate` (no crea tablas; valida)
  - Flyway crea/actualiza el esquema.

Documento recomendado:
- `docs/persistencia-postgres.md`

---

## 9) Configuración por ambientes

Hay un `application.yaml` base (perfil por defecto `dev`) y YAML por ambiente.

Puntos clave:
- `server.servlet.context-path` default: `/rfid-inventory`
- variables por entorno para BD, JWT secret, TTLs y stores

Ejemplo (ver `src/main/resources/application.yaml`):
```yaml
server:
  servlet:
    context-path: ${APP_CONTEXT:/rfid-inventory}
```

---

## 10) Patrones de diseño usados (ejemplos típicos)

- **Ports & Adapters (Hexagonal):** separación clara entre casos de uso y detalles técnicos.
- **Repository:** interfaces en aplicación/dominio y adaptadores JPA en infraestructura.
- **Mapper/Assembler:** traducción entre entidades JPA ↔ dominio ↔ DTO.
- **DTO + Bean Validation:** validación de datos en el borde de entrada (REST).
- **Strategy (configurable):** stores intercambiables (por ejemplo refresh token store `redis` vs `memory`).

Documento recomendado:
- `docs/patterns-and-design.md`

---

## 11) Principios SOLID (cómo se aplican aquí)

- **S (Single Responsibility):** controllers solo orquestan HTTP; use cases implementan reglas; adapters resuelven detalles técnicos.
- **O (Open/Closed):** se agregan adaptadores (p.ej. store Redis) sin reescribir casos de uso.
- **L (Liskov):** las implementaciones de puertos deben respetar el contrato del puerto.
- **I (Interface Segregation):** puertos pequeños y específicos (evita “mega interfaces”).
- **D (Dependency Inversion):** la aplicación depende de abstracciones (ports), no de JPA/Redis directamente.

Documento recomendado:
- `docs/solid-examples.md`

---

## 12) ¿Dónde empiezo si soy nuevo/a?

1) Leer `README.md` (raíz)
2) Leer `docs/ARQUITECTURA_Y_CONSTRUCCION.md`
3) Revisar `docs/ENDPOINTS_TOKENS.md`
4) Ejecutar el proyecto en local con Docker (ver `docs/docker-deployment.md`)
5) Probar endpoints en Swagger UI (`/swagger-ui.html` bajo el context-path)

---

Fin de `RESUMEN_PROYECTO.md`.

Versión: 2.0  
Fecha: 2026-05-15  
Actualizado: Documentación con 7 dominios actuales del proyecto

