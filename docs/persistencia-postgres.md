# PostgreSQL (aprovisionamiento)

Este proyecto usa **Flyway** para manejar migraciones en tiempo de ejecución, pero para entornos nuevos (o para preparar una DB local/CI) puedes usar un **script único** con:

- Creación de usuario/rol
- Creación de base de datos
- Creación de schema dedicado
- Asignación de privilegios
- Tablas + constraints + índices
- Seed mínimo (roles/permisos)

## Script de bootstrap

Archivo:

- `docs/db/bootstrap_postgres.sql`

### Ejecutar (DBeaver)

El script está pensado para ejecutarse en DBeaver en **2 pasos**:

1. Conectado a una DB existente (ej. `postgres`) ejecuta el **PASO 1** (creación de DB).
2. Abre una nueva conexión a `rfidinventorybd` y ejecuta el **PASO 2** (usuario, schema, privilegios, tablas y seed).

## Relación con Flyway

La estructura de tablas del script se mantiene alineada con las migraciones:

- `src/main/resources/db/migration/V1__init_users_roles.sql`
- `src/main/resources/db/migration/V2__init_permissions.sql`

Flyway seguirá ejecutándose al iniciar la app, pero como el script es idempotente y las migraciones usan `create table if not exists` + `on conflict do nothing`, no se generan conflictos.

## Regla de mantenimiento (cuando agregas un flujo nuevo)

Cada vez que se cree un **flujo nuevo** (nuevo endpoint/caso de uso) y se requiera un cambio en BD:

1. **Crear una migración Flyway** en `src/main/resources/db/migration/` (V3, V4, ...).
2. **Actualizar `docs/db/bootstrap_postgres.sql`**:
   - agregar las nuevas tablas/columnas/constraints/índices en la sección de tablas
   - si aplica, actualizar el seed (catálogos, roles/permisos por defecto)

Esto garantiza que:
- Flyway sirve para evolución incremental del esquema.
- `bootstrap_postgres.sql` sirve para preparar desde cero un entorno nuevo (local/QA).

---

> Convención: mantener los nombres de markdown en formato `kebab-case` (minúsculas y guiones), y que el título principal refleje el nombre del archivo.

Este documento describe cómo implementar persistencia con **PostgreSQL** en este repositorio, respetando **Arquitectura Hexagonal**.

> Si prefieres mantener el repo minimalista, esta guía puede vivir dentro de `docs/guia-desarrollo.md` (sección 5.1). Este archivo existe para tener una referencia dedicada y fácil de encontrar.

## 1) Principios (Hexagonal)
- En **dominio** solo existen **puertos** (interfaces) para persistencia: `domain/ports/out/*RepositoryPort`.
- En **infrastructure/out** se implementan esos puertos usando Spring Data JPA.
- La capa REST (`infrastructure/in/rest`) trabaja con DTOs; nunca devuelve `@Entity`.

> Nota: actualmente en el código puede estar como `adapters/out/...` y `adapters/in/rest/...`. Conceptualmente ambos pertenecen a `infrastructure/*`.

## 2) Dependencias (Maven)
Agregar (cuando corresponda) en `pom.xml`:
- `org.springframework.boot:spring-boot-starter-data-jpa`
- `org.postgresql:postgresql`
- `org.flywaydb:flyway-core`

## 3) Configuración (application.yaml)
Ejemplo base:
- `spring.datasource.url=jdbc:postgresql://HOST:5432/DB_NAME`
- `spring.datasource.username=...`
- `spring.datasource.password=...`
- `spring.jpa.hibernate.ddl-auto=validate`
- `spring.jpa.open-in-view=false`

Recomendaciones:
- Manejar credenciales por variables de entorno o secrets manager.
- Definir perfiles: `application-dev.yaml`, `application-prod.yaml`.

## 4) Migraciones (Flyway) - Estado ACTUAL del proyecto

Ubicación:
- `src/main/resources/db/migration/`

### Migraciones actuales del proyecto (13 versiones)

El proyecto tiene estas **13 migraciones Flyway** que definen el esquema completo de datos:

```
V1__create_schema_and_extensions.sql      # Crea schema rfid_inventory, extensiones UUID y JSON
V2__create_and_seed_companies.sql         # Tabla companies (empresas)
V3__create_and_seed_warehouse_types.sql   # Tabla warehouse_types (tipos de bodega)
V4__create_and_seed_roles.sql             # Tabla roles (ADMINISTRADOR, SUPERVISOR, etc.)
V5__create_and_seed_permissions.sql       # Tabla permissions (permisos finos)
V6__create_and_seed_inventory_status_types.sql  # Tabla inventory_status_types (estados de inventario)
V7__create_and_seed_rfid_models.sql       # Tabla rfid_models (modelos de lectores)
V8__create_and_seed_users.sql             # Tabla users (usuarios del sistema)
V9__create_and_seed_warehouses.sql        # Tabla warehouses (bodegas con FK a companies)
V10__create_and_seed_rfid_readers.sql     # Tabla rfid_readers (lectores RFID con FK a warehouse)
V11__create_and_seed_inventories.sql      # Tabla inventories (inventarios con FK a warehouse)
V12__create_and_seed_inventory_details.sql # Tabla inventory_details (items en inventario)
V13__create_and_seed_role_permissions.sql # Tabla role_permissions (relación many-to-many)
```

### Mapa de tablas por dominio

| Dominio | Tablas | Estado |
|---------|--------|--------|
| **user** | users, roles, permissions, role_permissions | ✅ Migraciones V4, V5, V8, V13 |
| **company** | companies | ✅ Migración V2 |
| **warehouse** | warehouses, warehouse_types | ✅ Migraciones V3, V9 |
| **rfidmodel** | rfid_models | ✅ Migración V7 |
| **rfidreader** | rfid_readers | ✅ Migración V10 |
| **inventory** | inventories, inventory_status_types | ✅ Migraciones V6, V11 |
| **inventorydetail** | inventory_details | ✅ Migración V12 |

### Estructura de paquetes en persistence (por dominio)

Bajo `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/`:

```
├── user/
│   ├── adapter/UserPersistenceAdapter.java
│   ├── entity/{UserEntity, RoleEntity, PermissionEntity}
│   ├── repository/{JpaUserRepository, JpaRoleRepository, JpaPermissionRepository}
│   └── mapper/UserPersistenceMapper.java
│
├── company/
│   ├── adapter/CompanyPersistenceAdapter.java
│   ├── entity/CompanyEntity.java
│   ├── repository/JpaCompanyRepository.java
│   └── mapper/CompanyPersistenceMapper.java
│
├── warehouse/
│   ├── adapter/WarehousePersistenceAdapter.java
│   ├── entity/{WarehouseEntity, WarehouseTypeEntity}
│   ├── repository/{JpaWarehouseRepository, JpaWarehouseTypeRepository}
│   └── mapper/WarehousePersistenceMapper.java
│
├── rfidmodel/
│   ├── adapter/RfidModelPersistenceAdapter.java
│   ├── entity/RfidModelEntity.java
│   ├── repository/JpaRfidModelRepository.java
│   └── mapper/RfidModelPersistenceMapper.java
│
├── rfidreader/
│   ├── adapter/RfidReaderPersistenceAdapter.java
│   ├── entity/RfidReaderEntity.java
│   ├── repository/JpaRfidReaderRepository.java
│   └── mapper/RfidReaderPersistenceMapper.java
│
├── inventory/
│   ├── adapter/InventoryPersistenceAdapter.java
│   ├── entity/{InventoryEntity, InventoryStatusTypeEntity}
│   ├── repository/{JpaInventoryRepository, JpaInventoryStatusTypeRepository}
│   └── mapper/{InventoryPersistenceMapper, InventoryStatusTypePersistenceMapper}
│
└── inventorydetail/
    ├── adapter/InventoryDetailPersistenceAdapter.java
    ├── entity/InventoryDetailEntity.java
    ├── repository/JpaInventoryDetailRepository.java
    └── mapper/InventoryDetailPersistenceMapper.java
```

### Configuración Flyway en `application.yaml` (ACTUAL)

Actualmente el proyecto está configurado así (ver `src/main/resources/application.yaml`):

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    schemas: rfid_inventory
    validate-on-migrate: true
```

Qué significa cada propiedad:

- `enabled: true`
  - Activa Flyway al iniciar la aplicación.
  - En el arranque, Flyway ejecuta (si hace falta) las migraciones pendientes antes de inicializar JPA/Hibernate.

- `locations: classpath:db/migration`
  - Indica dónde buscar los scripts de migración.
  - En este repo los scripts viven en: `src/main/resources/db/migration/`.

- `baseline-on-migrate: true`
  - Útil cuando conectas Flyway a una base ya existente (por ejemplo creada con `bootstrap_postgres.sql`).
  - Si la BD no tiene tabla de historial de Flyway (`flyway_schema_history`), Flyway crea un **baseline** para poder empezar a versionar sin “re-aplicar” cambios ya existentes.
  - Recomendación: mantenerlo `true` para entornos locales/QA cuando el bootstrap se ejecuta manualmente; en CI/entornos totalmente nuevos normalmente no es necesario, pero no suele causar problemas si el esquema está limpio.

- `schemas: rfid_inventory`
  - Define el schema donde Flyway gestionará su historial y sobre el que ejecutará las migraciones.
  - Debe coincidir con:
    - el schema creado en `docs/db/bootstrap_postgres.sql`
    - `hibernate.default_schema` (para que JPA apunte al mismo schema)

- `validate-on-migrate: true`
  - Antes de aplicar migraciones, Flyway valida que:
    - las migraciones ya aplicadas no hayan cambiado (checksum),
    - y que el historial sea consistente.
  - Esto evita “drifts” (cambios manuales en scripts ya ejecutados) y aumenta la seguridad del despliegue.

> Importante: mantener `spring.jpa.hibernate.ddl-auto=validate` (como está en este proyecto) para que **Hibernate no modifique el esquema** y la única fuente de evolución sea Flyway.

Reglas:
- Un cambio de esquema = un script versionado.
- Incluir constraints e índices.

Ejemplos de tipos recomendados:
- `uuid` para IDs (o `bigint` con identity)
- `timestamptz` para fechas
- `jsonb` para estructuras flexibles (si aplica)

### Estándares recomendados (PostgreSQL)
- **DDL gestionado por migraciones**: evitar `ddl-auto=update` en cualquier entorno compartido; preferir `validate`.
- **Naming consistente**:
  - Tablas: `snake_case` plural o singular, pero consistente (p.ej. `users`, `roles`, `inventory_details`).
  - Columnas: `snake_case`.
  - Constraints: `pk_<tabla>`, `fk_<tabla>__<ref>`, `uq_<tabla>__<col(s)>`, `ix_<tabla>__<col(s)>`.
- **Auditoría** (recomendado): `created_at`, `updated_at`, `created_by`, `updated_by`.
- **Soft delete** (si aplica): `deleted_at` + filtros en repositorio; documentar decisión en `docs/arquitectura.md`.
- **Índices**: agregar índices alineados con consultas reales (búsquedas por RFID, por bodega, por estado de inventario, etc.).
- **Integridad**:
  - `NOT NULL` para invariantes.
  - `UNIQUE` para claves naturales (correo, código lector, etc.).
  - `CHECK` cuando aplique (estados, rangos).

### IDs y claves
- Preferir **UUID** si necesitas IDs distribuidos (con `uuid` en Postgres).
- Preferir **IDENTITY** (`generated by default as identity`) si usas numéricos.
- Definir estrategia en una sola parte del modelo (no mezclar sin motivo).

## 5) Estructura de paquetes sugerida
Bajo `src/main/java/com/itm/rfid_inventory/`:
- `domain/ports/out/`
- `infrastructure/out/persistence/` (hoy puede existir como `adapters/out/persistence/`)
  - `entity/`
  - `repository/` (Spring Data)
  - `adapter/` (implementaciones de puertos)
  - `mapper/`

## 6) Patrón recomendado (Port → Adapter → JPA)
1. **Puerto** en dominio: `UserRepositoryPort`.
2. **Entidad JPA**: `UserEntity`.
3. **Repositorio Spring Data**: `JpaUserRepository extends JpaRepository<UserEntity, UUID>`.
4. **Adapter**: `UserRepositoryAdapter implements UserRepositoryPort` usando el JPA repository y un mapper.

## 6.1) Estándares JPA/Spring Data
- **Transacciones**: declarar transacciones en *use cases* (capa `application`) o en servicios de aplicación; evitar lógica transaccional en controllers.
- **N+1**:
  - usar `@EntityGraph` o `join fetch` cuando sea necesario.
  - evitar `EAGER` por defecto; preferir `LAZY` y cargar explícitamente.
- **Paginación**: para listados grandes, usar `Pageable` a nivel adapter REST y mapear a puertos.
- **Optimistic locking**: considerar `@Version` en entidades con concurrencia (inventarios, asignación de lectores, etc.).
- **Manejo de fechas**: preferir `Instant`/`OffsetDateTime` y persistir como `timestamptz`.
- **open-in-view**: mantener `spring.jpa.open-in-view=false` (ya recomendado) para evitar accesos a DB desde la capa web.

## 6.2) Errores típicos y cómo estandarizar la respuesta
- Violación `UNIQUE` → mapear a `409 Conflict`.
- No encontrado → `404`.
- Estado inválido (p.ej. inventario cerrado) → `409` o `422` según convención definida.
- No filtrar mensajes crudos del driver/DB al cliente.

## 6.3) Seguridad y datos sensibles
- No persistir contraseñas en texto plano: **BCrypt**.
- Evitar logs con PII (correo, documento, EPC) o enmascararlos.
- Validar inputs (Bean Validation) antes de llegar a persistencia.

## 6.5) Validación en endpoints (recomendado en Spring Boot)
En Spring Boot 3.x la validación recomendada es **Jakarta Bean Validation** (`jakarta.validation`, históricamente conocida como JSR 380).

### Estándar
- Validar **forma** y **restricciones simples** en la capa REST (DTOs) usando anotaciones (`@NotNull`, `@NotBlank`, `@Size`, `@Email`, `@Min`, `@Max`, etc.).
- Validar **invariantes de negocio** en el dominio (p.ej. “inventario no puede cerrarse si no tiene detalles”).
- En controllers, activar validación con `@Valid` (o `@Validated`) en `@RequestBody` y `@PathVariable`/`@RequestParam`.
- Centralizar errores de validación con un `@RestControllerAdvice` para responder un error consistente.

### Dependencia
Si agregas web starter, normalmente viene resuelto, pero para asegurar validación agrega:
- `org.springframework.boot:spring-boot-starter-validation`

### Ejemplo (DTO + Controller)
```java
public record CreateUserRequest(
  @jakarta.validation.constraints.NotBlank
  @jakarta.validation.constraints.Size(min = 3, max = 120)
  String username,

  @jakarta.validation.constraints.NotBlank
  @jakarta.validation.constraints.Email
  String email
) {}

@org.springframework.web.bind.annotation.RestController
class UserController {

  @org.springframework.web.bind.annotation.PostMapping("/api/v1/users")
  public Object create(@jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody CreateUserRequest req) {
    // delegar a caso de uso (application)
    return null;
  }
}
```

### Notas
- Usa validación declarativa para **lo que es declarable**; evita “if” repetitivos en controllers.
- Para validaciones compuestas, usa `@AssertTrue`, validadores custom (`ConstraintValidator`) o reglas de dominio.

## 6.4) Observabilidad
- Loguear tiempos de consultas “lentas” (sin incluir parámetros sensibles).
- Usar correlation-id (si lo agregas a nivel web) para trazar requests → DB.

## 7) Testing de persistencia
- Unit tests: para dominio/usecases (sin DB).
- Integración: Testcontainers con PostgreSQL o una DB de pruebas.

Comandos:
```powershell
./mvnw.cmd test
```

### Recomendación (Testcontainers)
Cuando incorpores integración real:
- levantar PostgreSQL con Testcontainers
- ejecutar migraciones Flyway en el arranque del test
- validar que repos/adapters funcionan contra un motor real

## 8) Checklist rápido
- [ ] No hay anotaciones JPA en `domain`
- [ ] Puertos en `domain/ports/out`
- [ ] Implementación en `adapters/out/persistence`
- [ ] Flyway activo y `ddl-auto=validate`
- [ ] DTOs separados de Entities
- [ ] Tests de integración para repos/adapters

## 9) Enlaces internos
- Prompt operativo: `prompts/prompt-persistencia-postgres.md`
- Guía consolidada: `docs/guia-desarrollo.md`
- Arquitectura: `docs/arquitectura.md`

# Persistencia con PostgreSQL — guía técnica

Resumen
- Persistencia en PostgreSQL siguiendo Arquitectura Hexagonal: los repositorios del dominio son puertos y la implementación concreta (JPA) vive en los adaptadores de salida.

Migraciones
- Usar Flyway: scripts en `src/main/resources/db/migration`.
- Nombres claros y atómicos: `V{numero}__descripcion.sql`.
- Mantener idempotencia donde sea posible y favorecer migraciones pequeñas y revisables.

Modelado y mapeo
- Entidades JPA solo en `infrastructure/out/persistence`.
- Dominios puros en `domain/` sin anotaciones JPA.
- Mapear Domain ↔ Entity con mappers explícitos (MapStruct o mapeo manual).

Transacciones
- Controlar transacciones en la capa de aplicación (`@Transactional` en servicios de aplicación) o en adaptadores si aplica.
- Evitar transacciones largas que crucen múltiples servicios externos.

Índices y performance
- Indexar columnas usadas en filtros/joins.
- Revisar planes de consulta en queries críticas.
- Evitar cargas EAGER inesperadas; preferir LAZY y fetch joins controlados.

Pruebas
- Tests unitarios para casos de uso.
- Tests de integración con Testcontainers/Postgres para validar migraciones y consultas.

Prácticas operativas
- No almacenar credenciales en el repositorio.
- Hacer backups regulares y pruebas de restauración.

Ejemplo mínimo (pasos)
1. Crear puerto en dominio: `domain/ports/out/InventoryRepository.java` (save, find, delete).
2. Crear entidad JPA en infra: `infrastructure/out/persistence/InventoryEntity.java`.
3. Crear Spring Data repository: `infrastructure/out/persistence/SpringDataInventoryRepository extends JpaRepository<InventoryEntity, UUID>`.

---

## ACTUALIZACIÓN: Estado actual del proyecto (2026-05-15)

### Dominios y migraciones implementadas

El proyecto cuenta con **13 migraciones Flyway** completamente funcionales que definen:

1. **Schema y extensiones** (V1)
2. **7 Dominios de negocio** con sus tablas, relaciones y seeds:
   - user (usuarios, roles, permisos)
   - company (empresas)
   - warehouse (bodegas y tipos)
   - rfidmodel (modelos de lectores)
   - rfidreader (lectores RFID)
   - inventory (inventarios y estados)
   - inventorydetail (detalles de inventarios)

### Características adoptadas

✅ Flyway 11.20.3 con PostgreSQL 18.x compatible  
✅ Arquitectura Hexagonal: puertos en dominio, adapters en infraestructura  
✅ Mappers explícitos entre Domain ↔ Entity (sin MapStruct, mapeo manual)  
✅ Spring Data JPA con repositories específicos por dominio  
✅ DDL-auto=validate (Flyway es única fuente de verdad)  
✅ UUIDs para IDs distribuidos  
✅ Timestamps con `timestamptz` para auditoría  
✅ Índices en columnas de búsqueda (email, código, estado)  
✅ Constraints: NOT NULL, UNIQUE, FOREIGN KEY, CHECK

### Próximos pasos sugeridos

- Implementar soft-delete en tablas críticas (users, companies, warehouses)
- Agregar paginación a listados con `Pageable`
- Tests de integración con Testcontainers para migraciones
- Optimización de N+1 en consultas complejas (inventory + details)

Versión: 2.0  
Fecha: 2026-05-15
