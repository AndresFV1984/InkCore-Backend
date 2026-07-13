# ARQUITECTURA Y CONSTRUCCIÓN DEL PROYECTO RFID-INVENTORY
## Documento consolidado para IAs y desarrolladores

---

## ÍNDICE

1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Arquitectura Hexagonal (Visión General)](#arquitectura-hexagonal-visión-general)
3. [Mapa de Paquetes y Responsabilidades](#mapa-de-paquetes-y-responsabilidades)
4. [Flujo de una Petición HTTP (ejemplo real)](#flujo-de-una-petición-http-ejemplo-real)
5. [Patrones de Diseño Aplicados](#patrones-de-diseño-aplicados)
6. [Principios SOLID (Ejemplos Concretos)](#principios-solid-ejemplos-concretos)
7. [Stack Técnico y Dependencias](#stack-técnico-y-dependencias)
8. [Configuración y Perfiles](#configuración-y-perfiles)
9. [Persistencia (PostgreSQL + Flyway)](#persistencia-postgresql--flyway)
10. [Seguridad (JWT, Refresh Tokens)](#seguridad-jwt-refresh-tokens)
11. [Empaquetado y Despliegue](#empaquetado-y-despliegue)
12. [Comandos de Build y Ejecución (PowerShell)](#comandos-de-build-y-ejecución-powershell)
13. [Referencias de Archivos Clave](#referencias-de-archivos-clave)

---

## RESUMEN EJECUTIVO

**Nombre:** RFID Inventory Service  
**Tecnología:** Java 17 + Spring Boot 3.5.11  
**Arquitectura:** Hexagonal (Ports & Adapters)  
**Persistencia:** PostgreSQL + Flyway  
**Seguridad:** JWT + Refresh Tokens  
**Empaquetado:** JAR ejecutable (Spring Boot "fat jar")  
**Containerización:** Docker + Kubernetes (plantillas incluidas)

### Características principales
- Separación clara entre dominio, aplicación e infraestructura.
- Principios SOLID aplicados en toda la codebase.
- Inyección de dependencias para testabilidad.
- Validaciones en DTO y reglas de negocio en dominio.
- Migraciones automáticas con Flyway.
- Documentación automática con OpenAPI/Swagger.

---

## ARQUITECTURA HEXAGONAL (VISIÓN GENERAL)

### Qué es Hexagonal
La Arquitectura Hexagonal (Ports & Adapters) separa la lógica de negocio en capas independientes de frameworks y dependencias externas.

**Capas del proyecto:**
1. **Domain (Dominio):** Modelos, reglas de negocio y puertos (interfaces).
2. **Application (Aplicación):** Casos de uso que orquestan el dominio.
3. **Infrastructure In (Entrada):** Adaptadores REST, DTOs, validaciones.
4. **Infrastructure Out (Salida):** Persistencia, mappers, implementaciones de puertos.
5. **Infrastructure Security:** JWT, tokens, autenticación.

### Beneficios en este proyecto
- La lógica de negocio no depende de Spring, JPA o frameworks.
- Fácil cambiar persistencia (ej.: de PostgreSQL a MongoDB).
- Tests unitarios sin arrancar contexto completo.
- Escalabilidad: agregar nuevos adaptadores sin tocar dominio.

---

## MAPA DE PAQUETES Y RESPONSABILIDADES

### Estructura de directorios (ACTUAL)
```
src/main/java/com/itm/rfid_inventory/
├── domain/
│   ├── user/
│   │   ├── model/           # User, Role, Permission (entidades de dominio)
│   │   ├── ports/
│   │   │   └── out/         # UserRepositoryPort, RoleRepositoryPort (interfaces)
│   │
│   ├── company/
│   │   ├── model/           # Company (entidad de dominio)
│   │   └── ports/
│   │       └── out/         # CompanyRepositoryPort
│   │
│   ├── warehouse/
│   │   ├── model/           # Warehouse, WarehouseType (entidades de dominio)
│   │   └── ports/
│   │       └── out/         # WarehouseRepositoryPort
│   │
│   ├── rfidmodel/
│   │   ├── model/           # RfidModel (entidad de dominio)
│   │   └── ports/
│   │       └── out/         # RfidModelRepositoryPort
│   │
│   ├── rfidreader/
│   │   ├── model/           # RfidReader (entidad de dominio)
│   │   └── ports/
│   │       └── out/         # RfidReaderRepositoryPort
│   │
│   ├── inventory/
│   │   ├── model/           # Inventory, InventoryStatusType (entidades de dominio)
│   │   └── ports/
│   │       └── out/         # InventoryRepositoryPort, InventoryStatusTypeRepositoryPort
│   │
│   └── inventorydetail/
│       ├── model/           # InventoryDetail (entidad de dominio)
│       └── ports/
│           └── out/         # InventoryDetailRepositoryPort
│
├── application/
│   ├── user/
│   │   └── usecase/         # CreateUserUseCase, LoginUserUseCase, etc.
│   ├── auth/
│   │   └── usecase/         # RefreshAccessTokenUseCase, LogoutAllSessionsUseCase
│   ├── company/
│   │   └── usecase/         # Casos de uso de empresa
│   ├── warehouse/
│   │   └── usecase/         # Casos de uso de bodega
│   ├── rfidmodel/
│   │   └── usecase/         # Casos de uso de modelo RFID
│   ├── rfidreader/
│   │   └── usecase/         # Casos de uso de lector RFID
│   ├── inventory/
│   │   └── usecase/         # Casos de uso de inventario
│   └── inventorydetail/
│       └── usecase/         # Casos de uso de detalle de inventario
│
├── infrastructure/
│   ├── in/
│   │   ├── rest/
│   │   │   ├── users/       # UserController, CreateUserRequest, UserResponse
│   │   │   ├── auth/        # AuthController
│   │   │   ├── companies/   # CompanyController
│   │   │   ├── warehouses/  # WarehouseController
│   │   │   ├── rfidmodels/  # RfidModelController
│   │   │   ├── rfidreaders/ # RfidReaderController
│   │   │   ├── inventories/ # InventoryController
│   │   │   ├── envelope/    # ApiResponseFactory, ApiEnvelope
│   │   │   └── errors/      # GlobalExceptionHandler
│   │
│   ├── out/
│   │   ├── persistence/
│   │   │   ├── user/
│   │   │   │   ├── adapter/       # UserPersistenceAdapter
│   │   │   │   ├── entity/        # UserEntity
│   │   │   │   ├── repository/    # JpaUserRepository
│   │   │   │   └── mapper/        # UserPersistenceMapper
│   │   │   ├── company/
│   │   │   │   ├── adapter/       # CompanyPersistenceAdapter
│   │   │   │   ├── entity/        # CompanyEntity
│   │   │   │   ├── repository/    # JpaCompanyRepository
│   │   │   │   └── mapper/        # CompanyPersistenceMapper
│   │   │   ├── warehouse/
│   │   │   │   ├── adapter/
│   │   │   │   ├── entity/
│   │   │   │   ├── repository/
│   │   │   │   └── mapper/
│   │   │   ├── rfidmodel/
│   │   │   │   ├── adapter/
│   │   │   │   ├── entity/
│   │   │   │   ├── repository/
│   │   │   │   └── mapper/
│   │   │   ├── rfidreader/
│   │   │   │   ├── adapter/
│   │   │   │   ├── entity/
│   │   │   │   ├── repository/
│   │   │   │   └── mapper/
│   │   │   ├── inventory/
│   │   │   │   ├── adapter/
│   │   │   │   ├── entity/
│   │   │   │   ├── repository/
│   │   │   │   └── mapper/
│   │   │   └── inventorydetail/
│   │   │       ├── adapter/
│   │   │       ├── entity/
│   │   │       ├── repository/
│   │   │       └── mapper/
│   │
│   └── security/
│       ├── JwtTokenService
│       ├── UserTokenVersionService (interfaz)
│       ├── InMemoryUserTokenVersionService
│       ├── RedisUserTokenVersionService
│       ├── JwtAuthoritiesConverter
│       ├── SecurityConfig
│       └── [otros servicios de seguridad]
│
└── RfidInventoryServiceApplication.java  # Punto de entrada Spring Boot
```

### Paquete `domain` (Modelos y Puertos)
**Responsabilidad:** Definen entidades de negocio y contratos (puertos) que otros paquetes deben implementar.

**Dominios actuales del proyecto:**

1. **user/** — Gestión de usuarios, roles y permisos
   - `model/User.java, Role.java, Permission.java`
   - `ports/out/UserRepositoryPort.java, RoleRepositoryPort.java`

2. **company/** — Gestión de empresas
   - `model/Company.java`
   - `ports/out/CompanyRepositoryPort.java`

3. **warehouse/** — Gestión de almacenes/bodegas
   - `model/Warehouse.java, WarehouseType.java`
   - `ports/out/WarehouseRepositoryPort.java`

4. **rfidmodel/** — Gestión de modelos/tipos de lectores RFID
   - `model/RfidModel.java`
   - `ports/out/RfidModelRepositoryPort.java`

5. **rfidreader/** — Gestión de lectores RFID físicos
   - `model/RfidReader.java`
   - `ports/out/RfidReaderRepositoryPort.java`

6. **inventory/** — Gestión de inventarios
   - `model/Inventory.java, InventoryStatusType.java, InventoryOverview.java`
   - `ports/out/InventoryRepositoryPort.java, InventoryStatusTypeRepositoryPort.java`

7. **inventorydetail/** — Gestión de detalles de inventarios (items/artículos en un inventario)
   - `model/InventoryDetail.java`
   - `ports/out/InventoryDetailRepositoryPort.java`

**Características comunes a todos:**
- Clases POJO puras (sin @Entity ni anotaciones JPA).
- Contienen reglas de negocio (validaciones, invariantes).
- Los puertos son interfaces Java que definen "qué" necesitan, no "cómo".

### Paquete `application` (Casos de Uso)
**Responsabilidad:** Orquestar operaciones del dominio; coordinan flujos de negocio.

**Archivos clave:**
- `application/user/usecase/CreateUserUseCase.java` — Crear usuario (validar email, hashear contraseña, persistir).
- `application/user/usecase/LoginUserUseCase.java` — Autenticar usuario y generar JWT.
- `application/user/usecase/ListPermissionsUseCase.java` — Listar permisos.
- `application/auth/usecase/RefreshAccessTokenUseCase.java` — Refrescar token JWT con refresh token.

**Características:**
- Inyectados puertos (no implementaciones concretas).
- No hacen llamadas directas a JPA o Spring.
- Declaradas como `@Service` de Spring (orquestación).
- Usan constructor injection para recibir puertos.

**Ejemplo (pseudocódigo):**
```java
@Service
public class CreateUserUseCase {
    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    
    // Constructor injection
    public CreateUserUseCase(UserRepositoryPort userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    public User create(CreateUserRequest request) {
        // 1. Validar email único (usar puerto)
        if (userRepository.existsByMail(request.email())) {
            throw new UserAlreadyExistsException(request.email());
        }
        
        // 2. Crear dominio
        User user = new User(request.username(), request.email(), 
                            passwordEncoder.encode(request.password()));
        
        // 3. Persistir (delegar a puerto)
        return userRepository.save(user);
    }
}
```

### Paquete `infrastructure/in` (Adaptadores de Entrada - REST)
**Responsabilidad:** Recibir peticiones HTTP, validar DTOs, mapear a objetos de entrada para use-cases, retornar respuestas HTTP.

**Archivos clave:**
- `infrastructure/in/rest/users/UserController.java` — Endpoints REST para usuarios.
- `infrastructure/in/rest/users/CreateUserRequest.java` — DTO para crear usuario (validaciones).
- `infrastructure/in/rest/users/UserResponse.java` — DTO de respuesta.
- `infrastructure/in/rest/auth/AuthController.java` — Endpoints de autenticación.
- `infrastructure/in/rest/envelope/ApiResponseFactory.java` — Factory para envolver respuestas.
- `infrastructure/in/rest/errors/GlobalExceptionHandler.java` — Mapeo de excepciones a HTTP.

**Características:**
- Controllers usan `@RestController` de Spring.
- DTOs usan validación: `@NotBlank`, `@Email`, etc. (Jakarta Bean Validation).
- Nunca devuelven `@Entity` JPA directamente; siempre retornan DTOs.
- Excepciones de negocio se mapean a códigos HTTP (400, 404, 409, etc.).

**Ejemplo:**
```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    private final CreateUserUseCase createUserUseCase;
    
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        User created = createUserUseCase.create(req);
        return ResponseEntity.status(201).body(UserResponse.from(created));
    }
}
```

### Paquete `infrastructure/out` (Adaptadores de Salida - Persistencia)
**Responsabilidad:** Implementar puertos del dominio usando Spring Data JPA; mapear entre Entities y modelos de dominio.

**Subpaquetes:**

#### `infrastructure/out/persistence/user/`
- **adapter/UserPersistenceAdapter.java** — Implementa `UserRepositoryPort` usando `JpaUserRepository`.
- **repository/JpaUserRepository.java** — Spring Data repository (extiende `JpaRepository`).
- **entity/UserEntity.java** — Entidad JPA mapeada a tabla `users`.
- **mapper/UserPersistenceMapper.java** — Convierte `User` (dominio) ↔ `UserEntity` (JPA).

**Características:**
- **Adapter:** implementa interfaz (`UserRepositoryPort`) y orquesta JPA repository + mapper.
- **Entity:** tiene anotaciones JPA (`@Entity`, `@Table`, `@Column`, etc.); vive solo aquí.
- **Repository:** Spring Data, métodos de CRUD y consultas custom.
- **Mapper:** conversión pura entre capas (sin lógica de negocio).

**Ejemplo (adapter):**
```java
@Component
public class UserPersistenceAdapter implements UserRepositoryPort {
    
    private final JpaUserRepository jpaUserRepository;
    private final UserPersistenceMapper mapper;
    
    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        UserEntity saved = jpaUserRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<User> findById(String id) {
        return jpaUserRepository.findById(id)
                               .map(mapper::toDomain);
    }
    
    @Override
    public boolean existsByMail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }
}
```

### Paquete `infrastructure/security` (Servicios de Seguridad)
**Responsabilidad:** Implementar autenticación JWT, gestión de refresh tokens y versionado de tokens.

**Archivos clave:**
- `JwtTokenService.java` — Generar y validar JWT.
- `UserTokenVersionService.java` — Interfaz para versionado de tokens (invalidación masiva).
- `InMemoryUserTokenVersionService.java` — Implementación en memoria (dev).
- `RedisUserTokenVersionService.java` — Implementación con Redis (prod).
- `InMemoryRefreshTokenStore.java` — Store de refresh tokens en memoria.
- `RefreshTokenStore.java` — Interfaz para almacenar refresh tokens.

**Características:**
- JWT incluye user ID, roles y permissions.
- Refresh tokens pueden almacenarse en memoria o Redis (configurable).
- Token versioning permite invalidar todos los tokens de un usuario de una sola vez.
- Validación de tokens en filters de Spring Security.

---

## FLUJO DE UNA PETICIÓN HTTP (EJEMPLO REAL)

### Caso: Registrar un nuevo usuario

**Endpoint:** `POST /api/v1/users`  
**Payload:**
```json
{
  "username": "juan.perez",
  "email": "juan@example.com",
  "password": "SecurePass123!"
}
```

### Pasos del flujo

1. **Entrada (HTTP):**
   - Cliente envía petición.
   - `UserController` recibe petición.

2. **Validación DTO:**
   - Spring valida `CreateUserRequest` contra anotaciones (`@NotBlank`, `@Email`, etc.).
   - Si falla validación → respuesta 400 Bad Request (manejada por `GlobalExceptionHandler`).

3. **Invocación del Caso de Uso:**
   - `UserController` invoca `CreateUserUseCase.create(request)`.

4. **Lógica de Negocio (Dominio):**
   - `CreateUserUseCase` valida reglas de negocio:
     - ¿Email ya existe? Consulta al puerto `UserRepositoryPort.existsByMail(email)`.
     - Si existe → excepción `UserAlreadyExistsException` → respuesta 409 Conflict.
   - Crea entidad de dominio `User` con contraseña hasheada.

5. **Persistencia (Salida):**
   - `CreateUserUseCase` invoca `UserRepositoryPort.save(user)`.
   - `UserPersistenceAdapter` (implementación) mapea `User` → `UserEntity`.
   - `JpaUserRepository.save(entity)` persiste en PostgreSQL.
   - Mapper convierte `UserEntity` → `User` (dominio).
   - Resultado retorna al use-case.

6. **Respuesta:**
   - `UserController` mapea `User` → `UserResponse` DTO.
   - `ApiResponseFactory` envuelve en envelope (metadata, status, data).
   - Retorna 201 Created con `UserResponse`.

**Código (resumido):**
```
HTTP POST /api/v1/users
  ↓ [validación DTO]
UserController.create(CreateUserRequest)
  ↓
CreateUserUseCase.create(CreateUserRequest)
  ├─ UserRepositoryPort.existsByMail(email) [consulta]
  ├─ User user = new User(...)
  └─ UserRepositoryPort.save(user) [persistencia]
       ↓
       UserPersistenceAdapter.save(User)
         ├─ UserEntity entity = mapper.toEntity(user)
         ├─ JpaUserRepository.save(entity)
         └─ return mapper.toDomain(saved)
  ↓
UserController.create() → UserResponse → HTTP 201
```

---

## PATRONES DE DISEÑO APLICADOS

### 1. Ports & Adapters (Hexagonal)
- **Dónde:** `domain/*/ports/out` (puertos) y `infrastructure/out/persistence/*/adapter` (adapters).
- **Propósito:** Desacoplar dominio de dependencias externas.
- **Ejemplo:** `UserRepositoryPort` ↔ `UserPersistenceAdapter`.

### 2. Repository (Spring Data)
- **Dónde:** `infrastructure/out/persistence/*/repository/Jpa*Repository`.
- **Propósito:** CRUD y consultas sobre BD.
- **Ejemplo:** `JpaUserRepository extends JpaRepository<UserEntity, String>`.

### 3. Data Mapper
- **Dónde:** `infrastructure/out/persistence/*/mapper/*PersistenceMapper`.
- **Propósito:** Convertir entre capas (Domain ↔ Entity ↔ DTO).
- **Ejemplo:** `UserPersistenceMapper` convierte `User` ↔ `UserEntity`.

### 4. Factory
- **Dónde:** `infrastructure/in/rest/envelope/ApiResponseFactory`.
- **Propósito:** Crear respuestas HTTP estandarizadas.
- **Ejemplo:** `ApiResponseFactory.success(data)` → envuelve en `ApiEnvelope`.

### 5. Strategy (Conditional Beans)
- **Dónde:** `infrastructure/security/` (servicios de token).
- **Propósito:** Seleccionar implementación por configuración.
- **Ejemplo:** `InMemoryUserTokenVersionService` vs `RedisUserTokenVersionService` según `@ConditionalOnProperty`.

### 6. Handler (Exception Handler)
- **Dónde:** `infrastructure/in/rest/errors/GlobalExceptionHandler`.
- **Propósito:** Mapear excepciones a respuestas HTTP.
- **Ejemplo:** `@ExceptionHandler(UserAlreadyExistsException.class)` → 409 Conflict.

### 7. Dependency Injection (Constructor Injection)
- **Dónde:** Toda la aplicación.
- **Propósito:** Testabilidad y desacoplamiento.
- **Ejemplo:** `CreateUserUseCase(UserRepositoryPort, PasswordEncoder)`.

---

## PRINCIPIOS SOLID (EJEMPLOS CONCRETOS)

### Single Responsibility Principle (SRP)
**Definición:** Una clase debe tener una única razón para cambiar.

**Ejemplos en el proyecto:**
- `CreateUserUseCase` — responsable únicamente de orquestar creación de usuario.
- `UserPersistenceMapper` — responsable únicamente de mapeo User ↔ UserEntity.
- `UserController` — responsable únicamente de adaptar HTTP ↔ use-cases.

**Archivos:**
- `application/user/usecase/CreateUserUseCase.java`
- `infrastructure/out/persistence/user/mapper/UserPersistenceMapper.java`
- `infrastructure/in/rest/users/UserController.java`

### Open/Closed Principle (OCP)
**Definición:** Abierto para extensión, cerrado para modificación.

**Ejemplo:**
- `UserRepositoryPort` es una interfaz; se pueden crear nuevas implementaciones (ej.: `MongoUserPersistenceAdapter`) sin cambiar use-cases ni controladores.
- Nuevas estrategias de token versioning (`RedisUserTokenVersionService`) se agregaban sin modificar `JwtTokenService`.

**Archivos:**
- `domain/user/ports/out/UserRepositoryPort.java`
- `infrastructure/out/persistence/user/adapter/UserPersistenceAdapter.java`
- `infrastructure/security/InMemoryUserTokenVersionService.java` vs `RedisUserTokenVersionService.java`

### Liskov Substitution Principle (LSP)
**Definición:** Objetos de subclases deben sustituirse por objetos de superclase sin alterar la corrección.

**Ejemplo:**
- `UserPersistenceAdapter implements UserRepositoryPort`: mantiene contratos (firmas de métodos, tipos de retorno).
- Métodos como `save()`, `findById()`, `existsByMail()` siempre retornan tipos esperados.

**Archivo:**
- `infrastructure/out/persistence/user/adapter/UserPersistenceAdapter.java`

### Interface Segregation Principle (ISP)
**Definición:** Muchas interfaces específicas son mejores que una interfaz general.

**Ejemplo actual:**
- `UserRepositoryPort` agrupa operaciones de persistencia (save, find, exists, getTokenVersion).
- Recomendación: si crece, separar en `UserReadPort` y `UserWritePort` para que clientes no dependan de métodos innecesarios.

**Archivo:**
- `domain/user/ports/out/UserRepositoryPort.java`

### Dependency Inversion Principle (DIP)
**Definición:** Módulos de alto nivel no deben depender de módulos de bajo nivel; ambos deben depender de abstracciones.

**Ejemplo:**
- `CreateUserUseCase` (alto nivel) depende de `UserRepositoryPort` (abstracción), no de `JpaUserRepository` (bajo nivel).
- `UserPersistenceAdapter` (bajo nivel) implementa la abstracción `UserRepositoryPort`.

**Archivos:**
- `application/user/usecase/CreateUserUseCase.java` (constructor injection)
- `domain/user/ports/out/UserRepositoryPort.java`
- `infrastructure/out/persistence/user/adapter/UserPersistenceAdapter.java`

---

## STACK TÉCNICO Y DEPENDENCIAS

### Tecnologías core
- **Java:** 17 (LTS)
- **Spring Boot:** 3.5.11
- **Maven:** mvnw (Maven Wrapper)

### Dependencias principales (en `pom.xml`)
```
spring-boot-starter-web (REST + Tomcat embebido)
spring-boot-starter-security (autenticación y autorización)
spring-boot-starter-oauth2-resource-server (JWT)
spring-boot-starter-data-jpa (ORM Hibernate)
spring-boot-starter-validation (Bean Validation)
spring-boot-starter-actuator (métricas y health)
spring-boot-starter-data-redis (cache y refresh tokens)

postgresql (driver PostgreSQL)
flyway-core + flyway-database-postgresql (migraciones)

springdoc-openapi-starter-webmvc-ui (Swagger/OpenAPI)
logstash-logback-encoder (logs estructurados)

spring-boot-starter-test (JUnit 5, Mockito)
testcontainers + testcontainers-postgresql (tests de integración)
h2 (BD en memoria para tests)
```

### Overrides de seguridad en `pom.xml`
```properties
jackson.version=2.18.6          # CVE-2024 en jackson-core
commons-compress.version=1.26.0 # CVE-2024 en compress
flyway.version=11.20.3          # Compatible con PostgreSQL 18.x
postgresql.version=42.7.7       # Driver PostgreSQL reciente
```

### Empaquetado
```xml
<packaging>jar</packaging>  <!-- JAR ejecutable Spring Boot -->
<finalName>rfid-inventory</finalName>
```

---

## CONFIGURACIÓN Y PERFILES

### Perfiles Spring
El proyecto define perfiles para diferentes entornos:

- **dev** — `application-dev.yaml` — Local, BD en localhost, logs DEBUG, no Redis obligatorio.
- **prod** — `application-prod.yaml` — Producción, BD externa, logs INFO, Redis configurado.

### Archivos de configuración
```
src/main/resources/
├── application.yaml         # Config base (común a todos los perfiles)
├── application-dev.yaml     # Override para dev
├── application-prod.yaml    # Override para prod
└── logback-spring.xml       # Config de logging
```

### Propiedades clave (application.yaml)

**Base de datos:**
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate        # No modificar schema automáticamente
    open-in-view: false         # Evitar accesos lazy fuera de transacción
  flyway:
    enabled: true
    locations: classpath:db/migration
    schemas: rfid_inventory
```

**Seguridad:**
```yaml
security:
  jwt:
    secret: ${JWT_SECRET}
    expiration-seconds: ${JWT_EXP_SECONDS:3600}
  refresh-token:
    expiration-seconds: ${REFRESH_EXP_SECONDS:86400}
    store:
      type: ${REFRESH_TOKEN_STORE_TYPE:memory}  # memory | redis
```

**Server:**
```yaml
server:
  port: ${SERVER_PORT:8091}
  tomcat:
    threads:
      max: ${SERVER_TOMCAT_THREADS_MAX:50}
      min-spare: ${SERVER_TOMCAT_THREADS_MIN_SPARE:10}
```

### Variables de entorno (override en runtime)
```
SPRING_PROFILES_ACTIVE=dev|prod
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/db
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=pass
JWT_SECRET=tu-clave-secreta
JAVA_OPTS=-Xms256m -Xmx512m
```

---

## PERSISTENCIA (POSTGRESQL + FLYWAY)

### Estrategia de migraciones
El proyecto usa **Flyway** para versionamiento automático del esquema.

**Ubicación:** `src/main/resources/db/migration/`

### Migraciones existentes
```
V1__init_users_roles.sql              # Creación de users, roles, permissions
V2__init_permissions.sql              # Permisos específicos
V3__add_users_token_version.sql       # Versionado de tokens
V4__seed_users_if_empty.sql           # Seed de usuarios
...
V10__change_user_role_to_single.sql   # Cambios de diseño
```

### Configuración Flyway en `application.yaml`
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true           # Útil con BD preexistentes
    schemas: rfid_inventory
    validate-on-migrate: true           # Valida checksums de migraciones
```

### Modelado de entidades
- **Ubicación:** `infrastructure/out/persistence/*/entity/`.
- **Convención:** Nombres en `snake_case` (tablas y columnas).
- **Anotaciones JPA:** `@Entity`, `@Table`, `@Column`, `@ManyToOne`, etc.

**Ejemplo (UserEntity):**
```java
@Entity
@Table(name = "users", schema = "rfid_inventory")
public class UserEntity {
    
    @Id
    @Column(name = "id")
    private String id;
    
    @Column(name = "username", nullable = false, unique = true)
    private String username;
    
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;
    
    @Column(name = "token_version")
    private Long tokenVersion;
    
    // Auditoría (recomendado)
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
}
```

### Buenas prácticas aplicadas
- `ddl-auto: validate` — Flyway es la única fuente de evolución.
- No hay `@OneToMany` eager; preferir LAZY y `@EntityGraph` si es necesario.
- Índices explícitos en tablas para queries críticas (email, uuid, etc.).
- Constraints: `NOT NULL`, `UNIQUE`, `FOREIGN KEY` en DDL.

---

## SEGURIDAD (JWT, REFRESH TOKENS)

### Flujo de autenticación

1. **Login:**
   - Cliente envía credenciales (username/password) a `POST /api/v1/auth/login`.
   - `AuthController` invoca `LoginUserUseCase`.
   - Valida credenciales contra BD (BCrypt).
   - Genera JWT (access token) con claims: `sub` (user ID), `roles`, `permissions`.
   - Genera refresh token (opaco, almacenado en store).
   - Retorna `{ accessToken, refreshToken, expiresIn }`.

2. **Acceso a recursos protegidos:**
   - Cliente envía request con header `Authorization: Bearer <JWT>`.
   - Spring Security intercepta y valida JWT.
   - Si válido, extrae user ID y roles para autorización.
   - Permite acceso a recurso.

3. **Refresh de token:**
   - Cliente envía refresh token a `POST /api/v1/auth/refresh`.
   - `RefreshAccessTokenUseCase` valida refresh token.
   - Genera nuevo JWT.
   - Opcionalmente: rota refresh token (genera nuevo).

### Versionado de tokens (Token Invalidation)
Permite invalidar todos los tokens de un usuario de una sola vez (logout desde todos los dispositivos).

- **Mecanismo:** Cada JWT incluye `tokenVersion` (incremento por usuario).
- **Store:** En memoria (dev) o Redis (prod).
- **Uso:** `LogoutAllUserSessionsUseCase` incrementa la versión del usuario → todos sus tokens anteriores invalidan.

### Archivos clave
- `infrastructure/security/JwtTokenService.java` — Generación y validación de JWT.
- `infrastructure/security/UserTokenVersionService.java` — Interfaz de versionado.
- `infrastructure/security/InMemoryUserTokenVersionService.java` — Implementación en memoria.
- `infrastructure/security/RedisUserTokenVersionService.java` — Implementación en Redis.
- `application/auth/usecase/RefreshAccessTokenUseCase.java` — Refrescar token.
- `application/auth/usecase/LogoutAllUserSessionsUseCase.java` — Invalidar todos los tokens.

### Configuración (application.yaml)
```yaml
security:
  jwt:
    secret: ${JWT_SECRET:dev-only-change-me}
    expiration-seconds: ${JWT_EXP_SECONDS:3600}           # 1 hora
  refresh-token:
    expiration-seconds: ${REFRESH_EXP_SECONDS:86400}      # 24 horas
    store:
      type: ${REFRESH_TOKEN_STORE_TYPE:memory}
  token-version-cache:
    type: ${TOKEN_VERSION_CACHE_TYPE:memory}
    ttl-seconds: ${TOKEN_VERSION_CACHE_TTL_SECONDS:60}
```

---

## EMPAQUETADO Y DESPLIEGUE

### JAR ejecutable (Spring Boot)
- **Tipo:** JAR "fat jar" (contiene todas las dependencias).
- **Servidor embebido:** Tomcat 10.x.
- **Ubicación generada:** `target/rfid-inventory.jar`.

**Ventajas:**
- Despliegue simple: solo copiar JAR + JRE.
- Ideal para Docker, Kubernetes, microservicios.
- Cada versión completamente aislada.

### Docker
**Dockerfile multi-stage:**
1. **Stage build:** Compila con Maven en contenedor maven:3.9.6-eclipse-temurin-17.
2. **Stage runtime:** Ejecuta JAR en contenedor eclipse-temurin:17-jre con usuario no-root.

**Variables en imagen:**
```dockerfile
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:MaxRAMPercentage=75.0"
ENV SPRING_PROFILES_ACTIVE=prod
```

**Healthcheck:** `curl http://localhost:8091/actuator/health`

### Kubernetes
Plantillas disponibles en `k8s/`:
- `deployment.yaml` — Deployment con replicas, probes, recursos.
- `service.yaml` — Service ClusterIP puerto 8091.
- `secret.yaml` — Secretos (DB, JWT).
- `hpa.yaml` — HorizontalPodAutoscaler CPU-based.

---

## COMANDOS DE BUILD Y EJECUCIÓN (POWERSHELL)

### Compilar y empaquetar
```powershell
# Compilar sin tests
.\mvnw -DskipTests clean package

# Compilar con tests
.\mvnw clean package

# Compilar con integration tests (requiere Docker)
.\mvnw verify -DskipITs=false
```

### Ejecutar JAR local
```powershell
# Con perfil dev
java -jar .\target\rfid-inventory.jar --spring.profiles.active=dev

# Con perfil prod (requiere variables de entorno)
java -jar .\target\rfid-inventory.jar --spring.profiles.active=prod

# Con JAVA_OPTS adicionales
java -Xms512m -Xmx1g -jar .\target\rfid-inventory.jar
```

### Ejecutar con Spring Boot plugin
```powershell
# Dev local
.\mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Con argumentos personalizados
.\mvnw spring-boot:run -Dspring-boot.run.arguments="--server.port=9000"
```

### Tests
```powershell
# Unit tests
.\mvnw test

# Tests específicos
.\mvnw test -Dtest=CreateUserUseCaseTest

# Integration tests (requiere Docker)
.\mvnw verify -DskipITs=false
```

### Docker
```powershell
# Construir imagen
docker build -t rfid-inventory:latest .

# Ejecutar imagen
docker run --rm -p 8091:8091 `
  -e SPRING_PROFILES_ACTIVE=dev `
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://host:5432/rfidinventorybd" `
  -e SPRING_DATASOURCE_USERNAME=usrfidinventory `
  -e SPRING_DATASOURCE_PASSWORD=RfidInventory2026! `
  rfid-inventory:latest

# Con docker-compose (si existe)
docker-compose up --build
```

### Kubernetes
```powershell
# Aplicar secrets
kubectl apply -f .\k8s\secret.yaml

# Desplegar aplicación
kubectl apply -f .\k8s\deployment.yaml
kubectl apply -f .\k8s\service.yaml
kubectl apply -f .\k8s\hpa.yaml

# Ver status
kubectl get deployments,pods,services -l app=rfid-inventory

# Ver logs
kubectl logs -f deployment/rfid-inventory
```

---

## REFERENCIAS DE ARCHIVOS CLAVE

### Domain (Modelos y Puertos)

**User:**
- `src/main/java/com/itm/rfid_inventory/domain/user/model/User.java`
- `src/main/java/com/itm/rfid_inventory/domain/user/model/Role.java`
- `src/main/java/com/itm/rfid_inventory/domain/user/model/Permission.java`
- `src/main/java/com/itm/rfid_inventory/domain/user/ports/out/UserRepositoryPort.java`
- `src/main/java/com/itm/rfid_inventory/domain/user/ports/out/RoleRepositoryPort.java`

**Company:**
- `src/main/java/com/itm/rfid_inventory/domain/company/model/Company.java`
- `src/main/java/com/itm/rfid_inventory/domain/company/ports/out/CompanyRepositoryPort.java`

**Warehouse:**
- `src/main/java/com/itm/rfid_inventory/domain/warehouse/model/Warehouse.java`
- `src/main/java/com/itm/rfid_inventory/domain/warehouse/model/WarehouseType.java`
- `src/main/java/com/itm/rfid_inventory/domain/warehouse/ports/out/WarehouseRepositoryPort.java`

**RfidModel:**
- `src/main/java/com/itm/rfid_inventory/domain/rfidmodel/model/RfidModel.java`
- `src/main/java/com/itm/rfid_inventory/domain/rfidmodel/ports/out/RfidModelRepositoryPort.java`

**RfidReader:**
- `src/main/java/com/itm/rfid_inventory/domain/rfidreader/model/RfidReader.java`
- `src/main/java/com/itm/rfid_inventory/domain/rfidreader/ports/out/RfidReaderRepositoryPort.java`

**Inventory:**
- `src/main/java/com/itm/rfid_inventory/domain/inventory/model/Inventory.java`
- `src/main/java/com/itm/rfid_inventory/domain/inventory/model/InventoryStatusType.java`
- `src/main/java/com/itm/rfid_inventory/domain/inventory/ports/out/InventoryRepositoryPort.java`
- `src/main/java/com/itm/rfid_inventory/domain/inventory/ports/out/InventoryStatusTypeRepositoryPort.java`

**InventoryDetail:**
- `src/main/java/com/itm/rfid_inventory/domain/inventorydetail/model/InventoryDetail.java`
- `src/main/java/com/itm/rfid_inventory/domain/inventorydetail/ports/out/InventoryDetailRepositoryPort.java`

### Application (Use Cases)
- `src/main/java/com/itm/rfid_inventory/application/user/usecase/CreateUserUseCase.java`
- `src/main/java/com/itm/rfid_inventory/application/user/usecase/LoginUserUseCase.java`
- `src/main/java/com/itm/rfid_inventory/application/auth/usecase/RefreshAccessTokenUseCase.java`
- `src/main/java/com/itm/rfid_inventory/application/auth/usecase/LogoutAllUserSessionsUseCase.java`
- `src/main/java/com/itm/rfid_inventory/application/company/usecase/*`
- `src/main/java/com/itm/rfid_inventory/application/warehouse/usecase/*`
- `src/main/java/com/itm/rfid_inventory/application/rfidmodel/usecase/*`
- `src/main/java/com/itm/rfid_inventory/application/rfidreader/usecase/*`
- `src/main/java/com/itm/rfid_inventory/application/inventory/usecase/*`
- `src/main/java/com/itm/rfid_inventory/application/inventorydetail/usecase/*`

### Infrastructure In (REST)
- `src/main/java/com/itm/rfid_inventory/infrastructure/in/rest/users/UserController.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/in/rest/users/CreateUserRequest.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/in/rest/users/UserResponse.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/in/rest/auth/AuthController.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/in/rest/companies/CompanyController.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/in/rest/warehouses/WarehouseController.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/in/rest/rfidmodels/RfidModelController.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/in/rest/rfidreaders/RfidReaderController.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/in/rest/inventories/InventoryController.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/in/rest/envelope/ApiResponseFactory.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/in/rest/errors/GlobalExceptionHandler.java`

### Infrastructure Out (Persistence)
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/user/adapter/UserPersistenceAdapter.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/user/entity/UserEntity.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/user/repository/JpaUserRepository.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/user/mapper/UserPersistenceMapper.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/company/adapter/CompanyPersistenceAdapter.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/company/entity/CompanyEntity.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/company/repository/JpaCompanyRepository.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/warehouse/adapter/WarehousePersistenceAdapter.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/warehouse/entity/WarehouseEntity.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/warehouse/repository/JpaWarehouseRepository.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/rfidmodel/adapter/RfidModelPersistenceAdapter.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/rfidmodel/entity/RfidModelEntity.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/rfidmodel/repository/JpaRfidModelRepository.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/rfidreader/adapter/RfidReaderPersistenceAdapter.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/rfidreader/entity/RfidReaderEntity.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/rfidreader/repository/JpaRfidReaderRepository.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/inventory/adapter/InventoryPersistenceAdapter.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/inventory/entity/InventoryEntity.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/inventory/repository/JpaInventoryRepository.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/inventorydetail/adapter/InventoryDetailPersistenceAdapter.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/inventorydetail/entity/InventoryDetailEntity.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/out/persistence/inventorydetail/repository/JpaInventoryDetailRepository.java`

### Infrastructure Security
- `src/main/java/com/itm/rfid_inventory/infrastructure/security/JwtTokenService.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/security/UserTokenVersionService.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/security/InMemoryUserTokenVersionService.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/security/RedisUserTokenVersionService.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/security/JwtAuthoritiesConverter.java`
- `src/main/java/com/itm/rfid_inventory/infrastructure/security/SecurityConfig.java`

### Configuración
- `pom.xml` — Dependencias Maven (packaging: jar).
- `src/main/resources/application.yaml` — Config base.
- `src/main/resources/application-dev.yaml` — Config dev.
- `src/main/resources/application-prod.yaml` — Config prod.
- `src/main/resources/logback-spring.xml` — Logging.
- `src/main/resources/db/migration/*.sql` — Migraciones Flyway.

### Docker y Kubernetes
- `Dockerfile` — Multi-stage build.
- `docker-compose.yml` — Orquestación local.
- `k8s/deployment.yaml` — Kubernetes Deployment.
- `k8s/service.yaml` — Kubernetes Service.
- `k8s/secret.yaml` — Kubernetes Secrets.
- `k8s/hpa.yaml` — Horizontal Pod Autoscaler.

---

## RESUMEN EJECUTIVO PARA IAS

**Cuando le pases este documento a otra IA, dile:**

> "Este proyecto es un backend Java/Spring Boot con arquitectura hexagonal. Lee ARQUITECTURA_Y_CONSTRUCCION.md completo (es auto-contenido). Contiene:
> - Mapa de paquetes (domain, application, infrastructure.in, infrastructure.out, security).
> - Flujo de petición HTTP real (create user example).
> - Patrones aplicados (Ports & Adapters, Repository, Mapper, Factory, Strategy).
> - SOLID con ejemplos concretos y referencias a archivos Java.
> - Stack técnico (Java 17, Spring Boot 3.5.11, PostgreSQL, Flyway, JWT).
> - Configuración de perfiles (dev, prod).
> - Persistencia (JPA, Flyway, migraciones).
> - Seguridad (JWT, refresh tokens, token versioning).
> - Empaquetado JAR y despliegue Docker/Kubernetes.
> - Comandos listos para PowerShell (build, test, run, docker, kubectl).
> - Referencias exactas de archivos clave.
> 
> Usa este documento como referencia única para entender cómo está construido el proyecto. Si necesitas hacer cambios, refiere archivos por su ruta exacta y utiliza patrones ya observados."

---

**Fin de ARQUITECTURA_Y_CONSTRUCCION.md**

Versión: 2.0  
Fecha: 2026-05-15  
Actualizado: Documentación completa con 7 dominios actuales (user, company, warehouse, rfidmodel, rfidreader, inventory, inventorydetail)
Compatible con: Spring Boot 3.5.11, Java 17, PostgreSQL 13+, Flyway 11.20.3

