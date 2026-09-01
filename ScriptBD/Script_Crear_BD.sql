-- ============================================
-- 1. CREAR BASE DE DATOS
-- ============================================
CREATE DATABASE inkcore
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'es_CO.UTF-8'
    LC_CTYPE = 'es_CO.UTF-8'
    TEMPLATE = template0;

-- ============================================
-- 2. CREAR ROL/USUARIO
-- ============================================
-- Rol owner/DDL de la aplicación madre Indicolors. Único rol autorizado para
-- migraciones y cambios de esquema (no debe usarlo la app en producción).
-- IMPORTANTE: reemplaza <PASSWORD_OWNER_SEGURO> por un password real,
-- generado aleatoriamente (mínimo 20 caracteres, sin patrones de año/versión).
-- Ejemplo de generación segura: openssl rand -base64 24
CREATE ROLE indicolors_owner WITH
    LOGIN
    SUPERUSER
    CREATEDB
    CREATEROLE
    PASSWORD 'YyZUdfRUcjJmiFxrkr5DP77mZD4hlRo1';

-- Rol de la aplicación indicolors, con mínimo privilegio, dedicado
-- exclusivamente al schema indicolors.
-- IMPORTANTE: reemplaza <PASSWORD_APP_SEGURO> por un password real y distinto al anterior.
CREATE ROLE indicolors_app WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    PASSWORD '5p5g+OQu9X6/cdi23jLKiTqSpAfgWCiS';

GRANT CONNECT ON DATABASE inkcore TO indicolors_app;

-- Rol administrador general (aplicación madre InkCore). No pertenece a un
-- esquema/empresa en particular: tiene control sobre TODOS los esquemas
-- de esta base de datos, existentes y futuros, sin ser superusuario del
-- servidor. Recibe automáticamente los permisos de cualquier esquema
-- nuevo gracias al event trigger definido más abajo.
-- IMPORTANTE: reemplaza <PASSWORD_ADMIN_SEGURO> por un password real,
-- generado aleatoriamente (mínimo 20 caracteres, sin patrones de año/versión,
-- distinto a los anteriores). Ejemplo de generación segura: openssl rand -base64 24
CREATE ROLE inkcore_admin WITH
    LOGIN
    NOSUPERUSER
    CREATEDB
    CREATEROLE
    PASSWORD 'kpDh7QcJVviWaxU91wLYyv2wIMECIUUb';

-- Lo hacemos dueño de la base de datos completa
ALTER DATABASE inkcore OWNER TO inkcore_admin;

-- Hereda el control sobre todo lo que posea/cree el rol owner del esquema
-- indicolors (y de cualquier otro esquema/empresa que se agregue después,
-- siempre que su rol owner también se otorgue a inkcore_admin)
GRANT indicolors_owner TO inkcore_admin;

-- Event trigger: otorga automáticamente todos los privilegios a
-- inkcore_admin sobre cualquier esquema nuevo que se cree en esta base
-- de datos, sin necesidad de acordarse de hacerlo manualmente cada vez.
CREATE OR REPLACE FUNCTION inkcore_grant_admin_on_new_schema()
RETURNS event_trigger AS $$
DECLARE
    obj record;
BEGIN
    FOR obj IN SELECT * FROM pg_event_trigger_ddl_commands()
        WHERE command_tag = 'CREATE SCHEMA'
    LOOP
        EXECUTE format('GRANT ALL ON SCHEMA %I TO inkcore_admin', obj.schema_name);
    END LOOP;
END;
$$ LANGUAGE plpgsql;

CREATE EVENT TRIGGER inkcore_new_schema_trigger
    ON ddl_command_end
    WHEN TAG IN ('CREATE SCHEMA')
    EXECUTE FUNCTION inkcore_grant_admin_on_new_schema();

-- ============================================
-- 3. CONECTAR A LA BASE DE DATOS
-- ============================================
\connect inkcore;


-- 4. CREAR SCHEMA
CREATE SCHEMA IF NOT EXISTS indicolors
    AUTHORIZATION indicolors_owner;

-- 4.1 EXTENSIONES NECESARIAS
-- pgcrypto habilita gen_random_uuid(), usado como default en roles/permissions
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 5. PERMISOS BASE DE DATOS
GRANT ALL PRIVILEGES ON DATABASE inkcore TO indicolors_owner;
GRANT CONNECT ON DATABASE inkcore TO indicolors_app;
GRANT USAGE ON SCHEMA indicolors TO indicolors_app;

-- 6. PERMISOS SCHEMA
GRANT ALL PRIVILEGES ON SCHEMA indicolors TO indicolors_owner;
GRANT USAGE ON SCHEMA indicolors TO indicolors_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA indicolors
    GRANT ALL PRIVILEGES ON TABLES TO indicolors_owner;

ALTER DEFAULT PRIVILEGES IN SCHEMA indicolors
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO indicolors_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA indicolors
    GRANT ALL PRIVILEGES ON SEQUENCES TO indicolors_owner;

ALTER DEFAULT PRIVILEGES IN SCHEMA indicolors
    GRANT USAGE, SELECT ON SEQUENCES TO indicolors_app;

-- Objetos futuros creados por Flyway / DDL con el rol indicolors_owner
ALTER DEFAULT PRIVILEGES FOR ROLE indicolors_owner IN SCHEMA indicolors
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO indicolors_app;

ALTER DEFAULT PRIVILEGES FOR ROLE indicolors_owner IN SCHEMA indicolors
    GRANT USAGE, SELECT ON SEQUENCES TO indicolors_app;

-- 7. SEARCH PATH
ALTER DATABASE inkcore SET search_path TO indicolors, public;

-- 7.1 USUARIO DEDICADO AL ESQUEMA indicolors (usado por la app indicolors)
-- Fija el search_path por defecto de la sesión de este rol, para que
-- no dependa de que la app lo configure manualmente en cada conexión.
ALTER ROLE indicolors_app SET search_path TO indicolors;

-- Endurecimiento: el rol de la app no necesita nada en el schema public
-- (evita que pueda crear objetos ahí ni depender de él por accidente).
REVOKE CREATE ON SCHEMA public FROM indicolors_app;
REVOKE ALL ON SCHEMA public FROM indicolors_app;

-- 7.2 CREAR TABLA COMPAÑÍAS
CREATE TABLE indicolors.companies (
    company_id     CHARACTER VARYING(64)  NOT NULL DEFAULT gen_random_uuid()::text,
    name           CHARACTER VARYING(200) NOT NULL,
    nit            CHARACTER VARYING(32)  NOT NULL,
    phone          CHARACTER VARYING(32),
    state          BOOLEAN                NOT NULL DEFAULT TRUE,
    address        CHARACTER VARYING(255),
    city           CHARACTER VARYING(120),
    email          CHARACTER VARYING(320),
    creation_date  DATE                   NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT companies_pkey PRIMARY KEY (company_id),
    CONSTRAINT companies_nit_unique UNIQUE (nit),
    CONSTRAINT companies_email_unique UNIQUE (email)
);

-- 7.3 ÍNDICES COMPAÑÍAS
CREATE INDEX idx_companies_name ON indicolors.companies (name);
CREATE INDEX idx_companies_state ON indicolors.companies (state);
CREATE INDEX idx_companies_city ON indicolors.companies (city);

-- 7.4 COMENTARIOS COMPAÑÍAS
COMMENT ON TABLE indicolors.companies IS 'Tabla de compañías/empresas registradas en el sistema';
COMMENT ON COLUMN indicolors.companies.company_id IS 'Identificador único de la compañía';
COMMENT ON COLUMN indicolors.companies.name IS 'Razón social o nombre de la compañía';
COMMENT ON COLUMN indicolors.companies.nit IS 'Número de identificación tributaria de la compañía';
COMMENT ON COLUMN indicolors.companies.phone IS 'Teléfono de contacto de la compañía';
COMMENT ON COLUMN indicolors.companies.state IS 'True=Activa, False=Inactiva';
COMMENT ON COLUMN indicolors.companies.address IS 'Dirección física de la compañía';
COMMENT ON COLUMN indicolors.companies.city IS 'Ciudad/municipio de ubicación de la compañía';
COMMENT ON COLUMN indicolors.companies.email IS 'Correo electrónico de contacto de la compañía';
COMMENT ON COLUMN indicolors.companies.creation_date IS 'Fecha de registro de la compañía en el sistema';

-- 8. CREAR TABLA
CREATE TABLE indicolors.users (
    user_id               CHARACTER VARYING(64)        NOT NULL,
    company_id            CHARACTER VARYING(64)        NOT NULL,
    identification_number CHARACTER VARYING(64)        NOT NULL,
    document_type         CHARACTER VARYING(20)        NOT NULL,
    name                  CHARACTER VARYING(200)       NOT NULL,
    mail                  CHARACTER VARYING(320)       NOT NULL,
    contact               CHARACTER VARYING(300)       NOT NULL DEFAULT '',
    department            CHARACTER VARYING(100)       NOT NULL,
    city                  CHARACTER VARYING(100)       NOT NULL,
    address               CHARACTER VARYING(255),
    password_hash         CHARACTER VARYING(200)       NOT NULL,
    creation_date         DATE                         NOT NULL,
    state                 BOOLEAN                      NOT NULL DEFAULT TRUE,
    token_version         BIGINT                       NOT NULL DEFAULT 1,
    force_password_change BOOLEAN                      DEFAULT TRUE,
    password_changed_at   TIMESTAMP WITHOUT TIME ZONE,
    password_expires_at   TIMESTAMP WITHOUT TIME ZONE,
    failed_attempts       INTEGER                      DEFAULT 0,
    locked_until          TIMESTAMP WITHOUT TIME ZONE,
    last_login_at         TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT users_pkey PRIMARY KEY (user_id),
    CONSTRAINT users_mail_unique UNIQUE (mail),
    CONSTRAINT users_identification_unique UNIQUE (identification_number),
    CONSTRAINT users_failed_attempts_check CHECK (failed_attempts >= 0),
    CONSTRAINT users_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id)
);

-- 9. ÍNDICES
CREATE INDEX idx_users_company_id ON indicolors.users (company_id);
CREATE INDEX idx_users_mail ON indicolors.users (mail);
CREATE INDEX idx_users_state ON indicolors.users (state);
CREATE INDEX idx_users_document ON indicolors.users (document_type, identification_number);
CREATE INDEX idx_users_department_city ON indicolors.users (department, city);
CREATE INDEX IF NOT EXISTS idx_users_mail_lower ON indicolors.users (LOWER(mail));
CREATE INDEX IF NOT EXISTS idx_users_company_state ON indicolors.users (company_id, state);

-- 10. COMENTARIOS
COMMENT ON TABLE indicolors.users IS 'Tabla de usuarios del sistema';
COMMENT ON COLUMN indicolors.users.user_id IS 'Identificador único del usuario';
COMMENT ON COLUMN indicolors.users.company_id IS 'Identificador de la empresa';
COMMENT ON COLUMN indicolors.users.document_type IS 'Tipo de documento: CC, CE, TI, PA, NIT';
COMMENT ON COLUMN indicolors.users.mail IS 'Correo electrónico único del usuario';
COMMENT ON COLUMN indicolors.users.department IS 'Departamento de ubicación del usuario';
COMMENT ON COLUMN indicolors.users.city IS 'Ciudad/municipio de ubicación del usuario';
COMMENT ON COLUMN indicolors.users.password_hash IS 'Hash de la contraseña';
COMMENT ON COLUMN indicolors.users.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.users.failed_attempts IS 'Intentos fallidos de login';
COMMENT ON COLUMN indicolors.users.locked_until IS 'Fecha hasta la que está bloqueado el usuario';

-- ============================================
-- 12. ROLES
-- ============================================
CREATE TABLE indicolors.roles (
    role_id      UUID                    NOT NULL DEFAULT gen_random_uuid(),
    company_id   CHARACTER VARYING(64)   NOT NULL,
    name         CHARACTER VARYING(100)  NOT NULL,
    description  CHARACTER VARYING(255),
    state        BOOLEAN                 NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT roles_pkey PRIMARY KEY (role_id),
    CONSTRAINT roles_name_company_unique UNIQUE (company_id, name),
    CONSTRAINT roles_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id)
);

CREATE INDEX idx_roles_company_id ON indicolors.roles (company_id);
CREATE INDEX idx_roles_state ON indicolors.roles (state);

COMMENT ON TABLE indicolors.roles IS 'Catálogo de roles del sistema (ej. Operador, Supervisor)';
COMMENT ON COLUMN indicolors.roles.company_id IS 'Identificador de la empresa dueña del rol';
COMMENT ON COLUMN indicolors.roles.state IS 'True=Activo, False=Inactivo';

-- ============================================
-- 13. PERMISOS (catálogo)
-- ============================================
CREATE TABLE indicolors.permissions (
    permission_id UUID                    NOT NULL DEFAULT gen_random_uuid(),
    code          CHARACTER VARYING(100)  NOT NULL,
    name          CHARACTER VARYING(150)  NOT NULL,
    module        CHARACTER VARYING(100),
    description   CHARACTER VARYING(255),
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT permissions_pkey PRIMARY KEY (permission_id),
    CONSTRAINT permissions_code_unique UNIQUE (code)
);

CREATE INDEX idx_permissions_module ON indicolors.permissions (module);

COMMENT ON TABLE indicolors.permissions IS 'Catálogo global de permisos disponibles en el sistema';
COMMENT ON COLUMN indicolors.permissions.code IS 'Código único del permiso, usado por la app (ej. production.orders.create)';
COMMENT ON COLUMN indicolors.permissions.name IS 'Nombre visible del permiso (ej. Crear y editar órdenes de producción)';
COMMENT ON COLUMN indicolors.permissions.module IS 'Módulo/agrupación funcional del permiso (ej. produccion, pedidos)';


-- ============================================
-- 14. ROLES <-> PERMISOS (muchos a muchos)
-- ============================================
CREATE TABLE indicolors.role_permissions (
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,
    granted_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT role_permissions_pkey PRIMARY KEY (role_id, permission_id),
    CONSTRAINT role_permissions_role_fk
        FOREIGN KEY (role_id) REFERENCES indicolors.roles (role_id) ON DELETE CASCADE,
    CONSTRAINT role_permissions_permission_fk
        FOREIGN KEY (permission_id) REFERENCES indicolors.permissions (permission_id) ON DELETE CASCADE
);

CREATE INDEX idx_role_permissions_permission_id ON indicolors.role_permissions (permission_id);

COMMENT ON TABLE indicolors.role_permissions IS 'Relación N:M entre roles y permisos';


-- ============================================
-- 15. USUARIOS <-> ROLES (muchos a muchos)
-- ============================================
CREATE TABLE indicolors.user_roles (
    user_id     CHARACTER VARYING(64) NOT NULL,
    role_id     UUID                   NOT NULL,
    assigned_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT user_roles_pkey PRIMARY KEY (user_id, role_id),
    CONSTRAINT user_roles_user_fk
        FOREIGN KEY (user_id) REFERENCES indicolors.users (user_id) ON DELETE CASCADE,
    CONSTRAINT user_roles_role_fk
        FOREIGN KEY (role_id) REFERENCES indicolors.roles (role_id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_role_id ON indicolors.user_roles (role_id);

COMMENT ON TABLE indicolors.user_roles IS 'Relación N:M entre usuarios y roles (un usuario puede tener uno o varios roles)';

-- ============================================
-- 16. PERMISOS TABLAS
-- ============================================
GRANT ALL PRIVILEGES ON TABLE indicolors.users TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.users TO indicolors_app;

GRANT ALL PRIVILEGES ON TABLE indicolors.roles TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.roles TO indicolors_app;

GRANT ALL PRIVILEGES ON TABLE indicolors.permissions TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.permissions TO indicolors_app;

GRANT ALL PRIVILEGES ON TABLE indicolors.role_permissions TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.role_permissions TO indicolors_app;

GRANT ALL PRIVILEGES ON TABLE indicolors.user_roles TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.user_roles TO indicolors_app;

GRANT ALL PRIVILEGES ON TABLE indicolors.companies TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.companies TO indicolors_app;

-- ============================================
-- 17. CREAR TABLA CLIENTES (formulario "Nuevo cliente")
-- ============================================
CREATE TABLE indicolors.clients (
    client_id      CHARACTER VARYING(64)  NOT NULL DEFAULT gen_random_uuid()::text,
    company_id     CHARACTER VARYING(64)  NOT NULL,
    name           CHARACTER VARYING(200) NOT NULL,
    document_type  CHARACTER VARYING(20),
    identification CHARACTER VARYING(32),
    department     CHARACTER VARYING(100) NOT NULL,
    city           CHARACTER VARYING(120) NOT NULL,
    address        CHARACTER VARYING(255),
    phone          CHARACTER VARYING(32),
    email          CHARACTER VARYING(320),
    contact_person CHARACTER VARYING(200),
    state          BOOLEAN                NOT NULL DEFAULT TRUE,
    creation_date  DATE                   NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT clients_pkey PRIMARY KEY (client_id),
    CONSTRAINT clients_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT clients_document_type_check
        CHECK (document_type IS NULL OR document_type IN ('CC', 'CE', 'TI', 'PA', 'NIT')),
    CONSTRAINT clients_email_check
        CHECK (email IS NULL OR email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- 17.1 ÍNDICES CLIENTES
CREATE INDEX idx_clients_company_id ON indicolors.clients (company_id);
CREATE INDEX idx_clients_name ON indicolors.clients (name);
CREATE INDEX idx_clients_identification ON indicolors.clients (identification);
CREATE INDEX idx_clients_document ON indicolors.clients (document_type, identification);
CREATE INDEX idx_clients_department_city ON indicolors.clients (department, city);
CREATE INDEX idx_clients_state ON indicolors.clients (state);
CREATE INDEX IF NOT EXISTS idx_clients_company_state ON indicolors.clients (company_id, state);

-- 17.2 COMENTARIOS CLIENTES
COMMENT ON TABLE indicolors.clients IS 'Tabla de clientes registrados por cada compañía (formulario Nuevo cliente)';
COMMENT ON COLUMN indicolors.clients.client_id IS 'Identificador único del cliente';
COMMENT ON COLUMN indicolors.clients.company_id IS 'Identificador de la empresa dueña del registro del cliente';
COMMENT ON COLUMN indicolors.clients.name IS 'Nombre o razón social del cliente';
COMMENT ON COLUMN indicolors.clients.document_type IS 'Tipo de documento del cliente: CC, CE, TI, PA, NIT';
COMMENT ON COLUMN indicolors.clients.identification IS 'Número de documento (NIT o cédula) del cliente';
COMMENT ON COLUMN indicolors.clients.department IS 'Departamento de ubicación del cliente';
COMMENT ON COLUMN indicolors.clients.city IS 'Ciudad/municipio de ubicación del cliente';
COMMENT ON COLUMN indicolors.clients.address IS 'Dirección del cliente (calle, barrio, referencia)';
COMMENT ON COLUMN indicolors.clients.phone IS 'Teléfono de contacto del cliente';
COMMENT ON COLUMN indicolors.clients.email IS 'Correo electrónico de contacto del cliente';
COMMENT ON COLUMN indicolors.clients.contact_person IS 'Nombre de la persona de contacto principal del cliente';
COMMENT ON COLUMN indicolors.clients.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.clients.creation_date IS 'Fecha de registro del cliente en el sistema';

-- 17.3 PERMISOS CLIENTES
GRANT ALL PRIVILEGES ON TABLE indicolors.clients TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.clients TO indicolors_app;

-- ============================================
-- 18. REFUERZO DE PERMISOS indicolors_app (fix login)
-- ============================================
-- GRANT CONNECT, GRANT USAGE ON SCHEMA y los ALTER DEFAULT PRIVILEGES
-- FOR ROLE indicolors_owner ya existen en las secciones 5, 6 y 6 (bis),
-- por eso no se repiten aqui.
--
-- Lo que faltaba: DML sobre TODAS las tablas ya existentes en el schema
-- (login usa users, user_roles, roles, role_permissions, permissions).
-- Los GRANT anteriores en el script se hicieron tabla por tabla; este
-- cubre de una sola vez cualquier tabla que se haya quedado sin permiso
-- explicito (por orden de ejecucion, ejecucion parcial, etc.).
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA indicolors TO indicolors_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA indicolors TO indicolors_app;

-- ============================================
-- 19. CREAR TABLA VENDEDORES (formulario "Nuevo vendedor")
-- ============================================
CREATE TABLE indicolors.sellers (
    seller_id       CHARACTER VARYING(64)  NOT NULL DEFAULT gen_random_uuid()::text,
    company_id      CHARACTER VARYING(64)  NOT NULL,
    full_name       CHARACTER VARYING(200) NOT NULL,
    document_type   CHARACTER VARYING(20)  NOT NULL,
    identification  CHARACTER VARYING(32)  NOT NULL,
    email           CHARACTER VARYING(320) NOT NULL,
    phone           CHARACTER VARYING(32),
    department      CHARACTER VARYING(100) NOT NULL,
    city            CHARACTER VARYING(120) NOT NULL,
    address         CHARACTER VARYING(255),
    state           BOOLEAN                NOT NULL DEFAULT TRUE,
    creation_date   DATE                   NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT sellers_pkey PRIMARY KEY (seller_id),
    CONSTRAINT sellers_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT sellers_identification_company_unique UNIQUE (company_id, identification),
    CONSTRAINT sellers_document_type_check
        CHECK (document_type IN ('CC', 'CE', 'TI', 'PA', 'NIT')),
    CONSTRAINT sellers_email_check
        CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

-- 19.1 ÍNDICES VENDEDORES
CREATE INDEX idx_sellers_company_id ON indicolors.sellers (company_id);
CREATE INDEX idx_sellers_full_name ON indicolors.sellers (full_name);
CREATE INDEX idx_sellers_document ON indicolors.sellers (document_type, identification);
CREATE INDEX idx_sellers_department_city ON indicolors.sellers (department, city);
CREATE INDEX idx_sellers_state ON indicolors.sellers (state);
CREATE INDEX IF NOT EXISTS idx_sellers_company_state ON indicolors.sellers (company_id, state);
CREATE INDEX IF NOT EXISTS idx_sellers_email_lower ON indicolors.sellers (LOWER(email));

-- 19.2 COMENTARIOS VENDEDORES
COMMENT ON TABLE indicolors.sellers IS 'Tabla de vendedores registrados por cada compañía (formulario Nuevo vendedor)';
COMMENT ON COLUMN indicolors.sellers.seller_id IS 'Identificador único del vendedor';
COMMENT ON COLUMN indicolors.sellers.company_id IS 'Identificador de la empresa dueña del registro del vendedor';
COMMENT ON COLUMN indicolors.sellers.full_name IS 'Nombre completo del vendedor';
COMMENT ON COLUMN indicolors.sellers.document_type IS 'Tipo de documento del vendedor: CC, CE, TI, PA, NIT';
COMMENT ON COLUMN indicolors.sellers.identification IS 'Número de documento de identificación del vendedor';
COMMENT ON COLUMN indicolors.sellers.email IS 'Correo electrónico del vendedor';
COMMENT ON COLUMN indicolors.sellers.phone IS 'Teléfono / contacto del vendedor';
COMMENT ON COLUMN indicolors.sellers.department IS 'Departamento de ubicación del vendedor';
COMMENT ON COLUMN indicolors.sellers.city IS 'Ciudad/municipio de ubicación del vendedor';
COMMENT ON COLUMN indicolors.sellers.address IS 'Dirección del vendedor (calle, barrio, referencia)';
COMMENT ON COLUMN indicolors.sellers.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.sellers.creation_date IS 'Fecha de registro del vendedor en el sistema';

-- 19.3 PERMISOS VENDEDORES
GRANT ALL PRIVILEGES ON TABLE indicolors.sellers TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.sellers TO indicolors_app;

-- ============================================
-- 20. CREAR TABLA CUENTAS BANCARIAS (formulario "Nueva cuenta bancaria")
-- ============================================
CREATE TABLE indicolors.bank_accounts (
    account_id         CHARACTER VARYING(64)  NOT NULL DEFAULT gen_random_uuid()::text,
    company_id         CHARACTER VARYING(64)  NOT NULL,
    bank_name          CHARACTER VARYING(150) NOT NULL,
    account_type       CHARACTER VARYING(30)  NOT NULL,
    account_number     CHARACTER VARYING(50)  NOT NULL,
    holder_name        CHARACTER VARYING(200) NOT NULL,
    holder_nit         CHARACTER VARYING(32),
    include_in_pdf     BOOLEAN                NOT NULL DEFAULT TRUE,
    is_primary         BOOLEAN                NOT NULL DEFAULT FALSE,
    state              BOOLEAN                NOT NULL DEFAULT TRUE,
    creation_date      DATE                   NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT bank_accounts_pkey PRIMARY KEY (account_id),
    CONSTRAINT bank_accounts_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT bank_accounts_number_company_unique UNIQUE (company_id, account_number),
    CONSTRAINT bank_accounts_type_check
        CHECK (account_type IN ('Ahorros', 'Corriente'))
);

-- 20.1 ÍNDICES CUENTAS BANCARIAS
CREATE INDEX idx_bank_accounts_company_id ON indicolors.bank_accounts (company_id);
CREATE INDEX idx_bank_accounts_state ON indicolors.bank_accounts (state);
CREATE INDEX idx_bank_accounts_bank_name ON indicolors.bank_accounts (bank_name);
CREATE INDEX idx_bank_accounts_company_state ON indicolors.bank_accounts (company_id, state);

-- Garantiza que cada compañía tenga como máximo UNA cuenta marcada
-- como principal (columna "Principal" del listado).
CREATE UNIQUE INDEX idx_bank_accounts_one_primary_per_company
    ON indicolors.bank_accounts (company_id)
    WHERE is_primary = TRUE;

-- 20.2 COMENTARIOS CUENTAS BANCARIAS
COMMENT ON TABLE indicolors.bank_accounts IS 'Cuentas bancarias de la compañía, usadas para cobros y generación de PDF de costeo al cliente';
COMMENT ON COLUMN indicolors.bank_accounts.account_id IS 'Identificador único de la cuenta bancaria';
COMMENT ON COLUMN indicolors.bank_accounts.company_id IS 'Identificador de la empresa dueña de la cuenta';
COMMENT ON COLUMN indicolors.bank_accounts.bank_name IS 'Nombre del banco (ej. Bancolombia)';
COMMENT ON COLUMN indicolors.bank_accounts.account_type IS 'Tipo de cuenta: Ahorros o Corriente';
COMMENT ON COLUMN indicolors.bank_accounts.account_number IS 'Número de la cuenta bancaria';
COMMENT ON COLUMN indicolors.bank_accounts.holder_name IS 'Nombre o razón social del titular de la cuenta';
COMMENT ON COLUMN indicolors.bank_accounts.holder_nit IS 'NIT del titular de la cuenta, si aplica';
COMMENT ON COLUMN indicolors.bank_accounts.include_in_pdf IS 'True=Incluir esta cuenta en el PDF de costeo al cliente (campo "Uso en documentos")';
COMMENT ON COLUMN indicolors.bank_accounts.is_primary IS 'True=Cuenta principal de la compañía (solo una por compañía)';
COMMENT ON COLUMN indicolors.bank_accounts.state IS 'True=Activa, False=Inactiva';
COMMENT ON COLUMN indicolors.bank_accounts.creation_date IS 'Fecha de registro de la cuenta en el sistema';

-- 20.3 PERMISOS CUENTAS BANCARIAS
GRANT ALL PRIVILEGES ON TABLE indicolors.bank_accounts TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.bank_accounts TO indicolors_app;

-- ============================================
-- 21. CREAR TABLA PRODUCTOS TERMINADOS (formulario "Nuevo terminado")
-- ============================================
CREATE TABLE indicolors.finished_products (
    finished_product_id CHARACTER VARYING(64)  NOT NULL DEFAULT gen_random_uuid()::text,
    company_id           CHARACTER VARYING(64)  NOT NULL,
    name                 CHARACTER VARYING(150) NOT NULL,
    min_cost             NUMERIC(12,2),
    value_per_cm2        NUMERIC(6,2)            NOT NULL DEFAULT 0,
    quick_access         BOOLEAN                 NOT NULL DEFAULT FALSE,
    state                BOOLEAN                 NOT NULL DEFAULT TRUE,
    creation_date        DATE                    NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT finished_products_pkey PRIMARY KEY (finished_product_id),
    CONSTRAINT finished_products_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT finished_products_name_company_unique UNIQUE (company_id, name),
    CONSTRAINT finished_products_min_cost_check
        CHECK (min_cost IS NULL OR min_cost >= 0),
    CONSTRAINT finished_products_value_per_cm2_check
        CHECK (value_per_cm2 >= 0 AND value_per_cm2 <= 9999)
);

-- 21.1 ÍNDICES PRODUCTOS TERMINADOS
CREATE INDEX idx_finished_products_company_id ON indicolors.finished_products (company_id);
CREATE INDEX idx_finished_products_name ON indicolors.finished_products (name);
CREATE INDEX idx_finished_products_state ON indicolors.finished_products (state);
CREATE INDEX idx_finished_products_company_state ON indicolors.finished_products (company_id, state);
CREATE INDEX idx_finished_products_quick_access ON indicolors.finished_products (quick_access);

-- 21.2 COMENTARIOS PRODUCTOS TERMINADOS
COMMENT ON TABLE indicolors.finished_products IS 'Catálogo de Productos Terminados (ej. Laminado mate), usados al configurar órdenes de producción';
COMMENT ON COLUMN indicolors.finished_products.finished_product_id IS 'Identificador único del producto terminado';
COMMENT ON COLUMN indicolors.finished_products.company_id IS 'Identificador de la empresa dueña del producto terminado';
COMMENT ON COLUMN indicolors.finished_products.name IS 'Nombre del producto terminado (ej. Laminado mate)';
COMMENT ON COLUMN indicolors.finished_products.min_cost IS 'Costo mínimo del producto terminado; NULL si no aplica';
COMMENT ON COLUMN indicolors.finished_products.value_per_cm2 IS 'Valor por cm² del producto terminado; 0 si no aplica';
COMMENT ON COLUMN indicolors.finished_products.quick_access IS 'True=Muestra el producto terminado en la barra de selección rápida al configurar una orden de producción';
COMMENT ON COLUMN indicolors.finished_products.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.finished_products.creation_date IS 'Fecha de registro del producto terminado en el sistema';

-- 21.3 PERMISOS PRODUCTOS TERMINADOS
GRANT ALL PRIVILEGES ON TABLE indicolors.finished_products TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.finished_products TO indicolors_app;

-- ============================================
-- 22. CREAR TABLA PROCESOS DE ACABADO APLICADOS (formulario "Nueva operación de acabado")
-- ============================================
CREATE TABLE indicolors.finishing_processes (
    finishing_process_id CHARACTER VARYING(64)  NOT NULL DEFAULT gen_random_uuid()::text,
    company_id            CHARACTER VARYING(64)  NOT NULL,
    name                  CHARACTER VARYING(150) NOT NULL,
    min_cost              NUMERIC(12,2),
    value_per_cm2         NUMERIC(6,2)            NOT NULL DEFAULT 0,
    quick_access          BOOLEAN                 NOT NULL DEFAULT FALSE,
    state                 BOOLEAN                 NOT NULL DEFAULT TRUE,
    creation_date         DATE                    NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT finishing_processes_pkey PRIMARY KEY (finishing_process_id),
    CONSTRAINT finishing_processes_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT finishing_processes_name_company_unique UNIQUE (company_id, name),
    CONSTRAINT finishing_processes_min_cost_check
        CHECK (min_cost IS NULL OR min_cost >= 0),
    CONSTRAINT finishing_processes_value_per_cm2_check
        CHECK (value_per_cm2 >= 0 AND value_per_cm2 <= 9999)
);

-- 22.1 ÍNDICES PROCESOS DE ACABADO APLICADOS
CREATE INDEX idx_finishing_processes_company_id ON indicolors.finishing_processes (company_id);
CREATE INDEX idx_finishing_processes_name ON indicolors.finishing_processes (name);
CREATE INDEX idx_finishing_processes_state ON indicolors.finishing_processes (state);
CREATE INDEX idx_finishing_processes_company_state ON indicolors.finishing_processes (company_id, state);
CREATE INDEX idx_finishing_processes_quick_access ON indicolors.finishing_processes (quick_access);

-- 22.2 COMENTARIOS PROCESOS DE ACABADO APLICADOS
COMMENT ON TABLE indicolors.finishing_processes IS 'Catálogo de Procesos de Acabado Aplicados (ej. Plegar, Embolsar), usados al configurar órdenes de producción';
COMMENT ON COLUMN indicolors.finishing_processes.finishing_process_id IS 'Identificador único del proceso de acabado aplicado';
COMMENT ON COLUMN indicolors.finishing_processes.company_id IS 'Identificador de la empresa dueña del proceso de acabado aplicado';
COMMENT ON COLUMN indicolors.finishing_processes.name IS 'Nombre del proceso de acabado aplicado (ej. Plegar, Embolsar)';
COMMENT ON COLUMN indicolors.finishing_processes.min_cost IS 'Costo mínimo del proceso de acabado aplicado; NULL si no aplica';
COMMENT ON COLUMN indicolors.finishing_processes.value_per_cm2 IS 'Valor por cm² del proceso de acabado aplicado; 0 si no aplica';
COMMENT ON COLUMN indicolors.finishing_processes.quick_access IS 'True=Muestra el proceso de acabado aplicado en la barra de selección rápida al configurar una orden de producción';
COMMENT ON COLUMN indicolors.finishing_processes.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.finishing_processes.creation_date IS 'Fecha de registro del proceso de acabado aplicado en el sistema';

-- 22.3 PERMISOS PROCESOS DE ACABADO APLICADOS
GRANT ALL PRIVILEGES ON TABLE indicolors.finishing_processes TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.finishing_processes TO indicolors_app;

-- ============================================
-- 23. CREAR TABLA DESPIECES (formulario "Nuevo despiece")
-- ============================================
CREATE TABLE indicolors.cut_layouts (
    cut_layout_id     CHARACTER VARYING(64)  NOT NULL DEFAULT gen_random_uuid()::text,
    company_id        CHARACTER VARYING(64)  NOT NULL,
    name              CHARACTER VARYING(150) NOT NULL,
    width             NUMERIC(10,2)           NOT NULL,
    height            NUMERIC(10,2)           NOT NULL,
    unit              CHARACTER VARYING(10)   NOT NULL DEFAULT 'cm',
    pieces_per_sheet  INTEGER                 NOT NULL,
    state             BOOLEAN                 NOT NULL DEFAULT TRUE,
    creation_date     DATE                    NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT cut_layouts_pkey PRIMARY KEY (cut_layout_id),
    CONSTRAINT cut_layouts_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT cut_layouts_name_company_unique UNIQUE (company_id, name),
    CONSTRAINT cut_layouts_width_check CHECK (width > 0),
    CONSTRAINT cut_layouts_height_check CHECK (height > 0),
    CONSTRAINT cut_layouts_pieces_per_sheet_check CHECK (pieces_per_sheet > 0),
    CONSTRAINT cut_layouts_unit_check CHECK (unit IN ('cm', 'mm', 'in'))
);

-- 23.1 ÍNDICES DESPIECES
CREATE INDEX idx_cut_layouts_company_id ON indicolors.cut_layouts (company_id);
CREATE INDEX idx_cut_layouts_name ON indicolors.cut_layouts (name);
CREATE INDEX idx_cut_layouts_state ON indicolors.cut_layouts (state);
CREATE INDEX idx_cut_layouts_company_state ON indicolors.cut_layouts (company_id, state);

-- 23.2 COMENTARIOS DESPIECES
COMMENT ON TABLE indicolors.cut_layouts IS 'Catálogo de Despieces / diseños de corte (ej. Etiqueta 10x5 cm), usados para calcular piezas por pliego al configurar órdenes de producción';
COMMENT ON COLUMN indicolors.cut_layouts.cut_layout_id IS 'Identificador único del despiece';
COMMENT ON COLUMN indicolors.cut_layouts.company_id IS 'Identificador de la empresa dueña del despiece';
COMMENT ON COLUMN indicolors.cut_layouts.name IS 'Nombre del despiece (ej. Etiqueta); sin medidas, se muestran aparte';
COMMENT ON COLUMN indicolors.cut_layouts.width IS 'Ancho de la pieza';
COMMENT ON COLUMN indicolors.cut_layouts.height IS 'Alto de la pieza';
COMMENT ON COLUMN indicolors.cut_layouts.unit IS 'Unidad de medida del ancho/alto: cm, mm o in';
COMMENT ON COLUMN indicolors.cut_layouts.pieces_per_sheet IS 'Cantidad de piezas que caben en un pliego';
COMMENT ON COLUMN indicolors.cut_layouts.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.cut_layouts.creation_date IS 'Fecha de registro del despiece en el sistema';

-- 23.3 PERMISOS DESPIECES
GRANT ALL PRIVILEGES ON TABLE indicolors.cut_layouts TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.cut_layouts TO indicolors_app;

-- ============================================
-- 24. CREAR TABLA TIPOS DE PAPEL (formulario "Nuevo tipo de papel")
-- ============================================
CREATE TABLE indicolors.paper_types (
    paper_type_id   CHARACTER VARYING(64)  NOT NULL DEFAULT gen_random_uuid()::text,
    company_id      CHARACTER VARYING(64)  NOT NULL,
    name            CHARACTER VARYING(150) NOT NULL,
    width           NUMERIC(10,2)           NOT NULL,
    height          NUMERIC(10,2)           NOT NULL,
    unit            CHARACTER VARYING(10)   NOT NULL DEFAULT 'cm',
    is_coated       BOOLEAN                 NOT NULL DEFAULT FALSE,
    state           BOOLEAN                 NOT NULL DEFAULT TRUE,
    creation_date   DATE                    NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT paper_types_pkey PRIMARY KEY (paper_type_id),
    CONSTRAINT paper_types_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT paper_types_name_company_unique UNIQUE (company_id, name),
    CONSTRAINT paper_types_width_check CHECK (width > 0),
    CONSTRAINT paper_types_height_check CHECK (height > 0),
    CONSTRAINT paper_types_unit_check CHECK (unit IN ('cm', 'mm', 'in'))
);

-- 24.1 ÍNDICES TIPOS DE PAPEL
CREATE INDEX idx_paper_types_company_id ON indicolors.paper_types (company_id);
CREATE INDEX idx_paper_types_name ON indicolors.paper_types (name);
CREATE INDEX idx_paper_types_state ON indicolors.paper_types (state);
CREATE INDEX idx_paper_types_company_state ON indicolors.paper_types (company_id, state);
CREATE INDEX idx_paper_types_is_coated ON indicolors.paper_types (is_coated);

-- 24.2 COMENTARIOS TIPOS DE PAPEL
COMMENT ON TABLE indicolors.paper_types IS 'Catálogo de Tipos de papel, usados al configurar órdenes de producción';
COMMENT ON COLUMN indicolors.paper_types.paper_type_id IS 'Identificador único del tipo de papel';
COMMENT ON COLUMN indicolors.paper_types.company_id IS 'Identificador de la empresa dueña del tipo de papel';
COMMENT ON COLUMN indicolors.paper_types.name IS 'Nombre del tipo de papel';
COMMENT ON COLUMN indicolors.paper_types.width IS 'Ancho de la hoja/pliego';
COMMENT ON COLUMN indicolors.paper_types.height IS 'Alto de la hoja/pliego';
COMMENT ON COLUMN indicolors.paper_types.unit IS 'Unidad de medida del ancho/alto: cm, mm o in';
COMMENT ON COLUMN indicolors.paper_types.is_coated IS 'True=Papel esmaltado (tiene recubrimiento esmaltado)';
COMMENT ON COLUMN indicolors.paper_types.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.paper_types.creation_date IS 'Fecha de registro del tipo de papel en el sistema';

-- 24.3 PERMISOS TIPOS DE PAPEL
GRANT ALL PRIVILEGES ON TABLE indicolors.paper_types TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.paper_types TO indicolors_app;

-- ============================================
-- 25. TIPOS DE PAPEL <-> DESPIECES (muchos a muchos, con valor de corte)
-- ============================================
CREATE TABLE indicolors.paper_type_cut_layouts (
    paper_type_id CHARACTER VARYING(64) NOT NULL,
    cut_layout_id CHARACTER VARYING(64) NOT NULL,
    cut_value     NUMERIC(12,2),
    assigned_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT paper_type_cut_layouts_pkey PRIMARY KEY (paper_type_id, cut_layout_id),
    CONSTRAINT paper_type_cut_layouts_paper_type_fk
        FOREIGN KEY (paper_type_id) REFERENCES indicolors.paper_types (paper_type_id) ON DELETE CASCADE,
    CONSTRAINT paper_type_cut_layouts_cut_layout_fk
        FOREIGN KEY (cut_layout_id) REFERENCES indicolors.cut_layouts (cut_layout_id) ON DELETE CASCADE,
    CONSTRAINT paper_type_cut_layouts_cut_value_check
        CHECK (cut_value IS NULL OR cut_value >= 0)
);

CREATE INDEX idx_paper_type_cut_layouts_cut_layout_id ON indicolors.paper_type_cut_layouts (cut_layout_id);

COMMENT ON TABLE indicolors.paper_type_cut_layouts IS 'Relación N:M entre tipos de papel y despieces por pliego, con el valor de corte asociado a cada combinación';
COMMENT ON COLUMN indicolors.paper_type_cut_layouts.paper_type_id IS 'Identificador del tipo de papel';
COMMENT ON COLUMN indicolors.paper_type_cut_layouts.cut_layout_id IS 'Identificador del despiece por pliego asociado';
COMMENT ON COLUMN indicolors.paper_type_cut_layouts.cut_value IS 'Valor de corte para este despiece en este tipo de papel específico';

-- 25.1 PERMISOS TIPOS DE PAPEL <-> DESPIECES
GRANT ALL PRIVILEGES ON TABLE indicolors.paper_type_cut_layouts TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.paper_type_cut_layouts TO indicolors_app;

-- ============================================
-- 25.2 CREAR TABLA PROVEEDORES (formulario "Nuevo proveedor")
-- ============================================
CREATE TABLE indicolors.suppliers (
    supplier_id    CHARACTER VARYING(64)  NOT NULL DEFAULT gen_random_uuid()::text,
    company_id     CHARACTER VARYING(64)  NOT NULL,
    name           CHARACTER VARYING(200) NOT NULL,
    document_type  CHARACTER VARYING(20),
    identification CHARACTER VARYING(32),
    department     CHARACTER VARYING(100) NOT NULL,
    city           CHARACTER VARYING(120) NOT NULL,
    address        CHARACTER VARYING(255),
    phone          CHARACTER VARYING(32),
    email          CHARACTER VARYING(320),
    contact_person CHARACTER VARYING(200),
    state          BOOLEAN                NOT NULL DEFAULT TRUE,
    creation_date  DATE                   NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT suppliers_pkey PRIMARY KEY (supplier_id),
    CONSTRAINT suppliers_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT suppliers_document_type_check
        CHECK (document_type IS NULL OR document_type IN ('CC', 'CE', 'TI', 'PA', 'NIT')),
    CONSTRAINT suppliers_email_check
        CHECK (email IS NULL OR email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

CREATE INDEX idx_suppliers_company_id ON indicolors.suppliers (company_id);
CREATE INDEX idx_suppliers_name ON indicolors.suppliers (name);
CREATE INDEX idx_suppliers_identification ON indicolors.suppliers (identification);
CREATE INDEX idx_suppliers_document ON indicolors.suppliers (document_type, identification);
CREATE INDEX idx_suppliers_department_city ON indicolors.suppliers (department, city);
CREATE INDEX idx_suppliers_state ON indicolors.suppliers (state);
CREATE INDEX idx_suppliers_company_state ON indicolors.suppliers (company_id, state);

COMMENT ON TABLE indicolors.suppliers IS 'Tabla de proveedores registrados por cada compañía (formulario Nuevo proveedor)';
COMMENT ON COLUMN indicolors.suppliers.supplier_id IS 'Identificador único del proveedor';
COMMENT ON COLUMN indicolors.suppliers.company_id IS 'Identificador de la empresa dueña del registro del proveedor';
COMMENT ON COLUMN indicolors.suppliers.name IS 'Nombre o razón social del proveedor';
COMMENT ON COLUMN indicolors.suppliers.document_type IS 'Tipo de documento del proveedor: CC, CE, TI, PA, NIT';
COMMENT ON COLUMN indicolors.suppliers.identification IS 'Número de documento (NIT o cédula) del proveedor';
COMMENT ON COLUMN indicolors.suppliers.department IS 'Departamento de ubicación del proveedor';
COMMENT ON COLUMN indicolors.suppliers.city IS 'Ciudad/municipio de ubicación del proveedor';
COMMENT ON COLUMN indicolors.suppliers.address IS 'Dirección del proveedor (calle, barrio, referencia)';
COMMENT ON COLUMN indicolors.suppliers.phone IS 'Teléfono de contacto del proveedor';
COMMENT ON COLUMN indicolors.suppliers.email IS 'Correo electrónico de contacto del proveedor';
COMMENT ON COLUMN indicolors.suppliers.contact_person IS 'Nombre de la persona de contacto principal del proveedor';
COMMENT ON COLUMN indicolors.suppliers.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.suppliers.creation_date IS 'Fecha de registro del proveedor en el sistema';

GRANT ALL PRIVILEGES ON TABLE indicolors.suppliers TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.suppliers TO indicolors_app;

-- ============================================
-- 25.3 PAPER_TYPE_SUPPLIERS (relación tipos de papel - proveedores)
-- ============================================
CREATE TABLE indicolors.paper_type_suppliers (
    paper_type_id   VARCHAR(64)     NOT NULL,
    supplier_id     VARCHAR(64)     NOT NULL,
    sheet_value     NUMERIC(12,2)   NOT NULL,
    package_unit    INTEGER         NOT NULL,
    assigned_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT paper_type_suppliers_pkey PRIMARY KEY (paper_type_id, supplier_id),
    CONSTRAINT paper_type_suppliers_paper_type_fk
        FOREIGN KEY (paper_type_id) REFERENCES indicolors.paper_types (paper_type_id) ON DELETE CASCADE,
    CONSTRAINT paper_type_suppliers_supplier_fk
        FOREIGN KEY (supplier_id) REFERENCES indicolors.suppliers (supplier_id) ON DELETE CASCADE,
    CONSTRAINT paper_type_suppliers_sheet_value_check CHECK (sheet_value >= 0),
    CONSTRAINT paper_type_suppliers_package_unit_check CHECK (package_unit > 0)
);

CREATE INDEX idx_paper_type_suppliers_supplier_id
    ON indicolors.paper_type_suppliers (supplier_id);

COMMENT ON TABLE indicolors.paper_type_suppliers IS 'Relación N:M entre tipos de papel y proveedores, con valor hoja y unidad empaque por proveedor';
COMMENT ON COLUMN indicolors.paper_type_suppliers.paper_type_id IS 'Identificador del tipo de papel';
COMMENT ON COLUMN indicolors.paper_type_suppliers.supplier_id IS 'Identificador del proveedor asociado';
COMMENT ON COLUMN indicolors.paper_type_suppliers.sheet_value IS 'Valor de la hoja/pliego para este proveedor';
COMMENT ON COLUMN indicolors.paper_type_suppliers.package_unit IS 'Cantidad de hojas por unidad de empaque para este proveedor';

GRANT ALL PRIVILEGES ON TABLE indicolors.paper_type_suppliers TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.paper_type_suppliers TO indicolors_app;

-- ============================================
-- 26. CREAR TABLA TIPOS DE PLANCHA (formulario "Nuevo tipo de plancha")
-- ============================================
CREATE TABLE indicolors.plate_types (
    plate_type_id   CHARACTER VARYING(64)  NOT NULL DEFAULT gen_random_uuid()::text,
    company_id      CHARACTER VARYING(64)  NOT NULL,
    name            CHARACTER VARYING(150) NOT NULL,
    width           NUMERIC(10,2)           NOT NULL,
    height          NUMERIC(10,2)           NOT NULL,
    unit            CHARACTER VARYING(10)   NOT NULL DEFAULT 'cm',
    value           NUMERIC(12,2)           NOT NULL,
    state           BOOLEAN                 NOT NULL DEFAULT TRUE,
    creation_date   DATE                    NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT plate_types_pkey PRIMARY KEY (plate_type_id),
    CONSTRAINT plate_types_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT plate_types_name_company_unique UNIQUE (company_id, name),
    CONSTRAINT plate_types_width_check CHECK (width > 0),
    CONSTRAINT plate_types_height_check CHECK (height > 0),
    CONSTRAINT plate_types_unit_check CHECK (unit IN ('cm', 'mm', 'in')),
    CONSTRAINT plate_types_value_check CHECK (value >= 0)
);

-- 26.1 ÍNDICES TIPOS DE PLANCHA
CREATE INDEX idx_plate_types_company_id ON indicolors.plate_types (company_id);
CREATE INDEX idx_plate_types_name ON indicolors.plate_types (name);
CREATE INDEX idx_plate_types_state ON indicolors.plate_types (state);
CREATE INDEX idx_plate_types_company_state ON indicolors.plate_types (company_id, state);

-- 26.2 COMENTARIOS TIPOS DE PLANCHA
COMMENT ON TABLE indicolors.plate_types IS 'Catálogo de Tipos de plancha, usados al configurar órdenes de producción';
COMMENT ON COLUMN indicolors.plate_types.plate_type_id IS 'Identificador único del tipo de plancha';
COMMENT ON COLUMN indicolors.plate_types.company_id IS 'Identificador de la empresa dueña del tipo de plancha';
COMMENT ON COLUMN indicolors.plate_types.name IS 'Nombre del tipo de plancha (ej. Plancha estándar)';
COMMENT ON COLUMN indicolors.plate_types.width IS 'Ancho de la plancha';
COMMENT ON COLUMN indicolors.plate_types.height IS 'Alto de la plancha';
COMMENT ON COLUMN indicolors.plate_types.unit IS 'Unidad de medida del ancho/alto: cm, mm o in';
COMMENT ON COLUMN indicolors.plate_types.value IS 'Valor de la plancha, en pesos colombianos (COP)';
COMMENT ON COLUMN indicolors.plate_types.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.plate_types.creation_date IS 'Fecha de registro del tipo de plancha en el sistema';

-- 26.3 PERMISOS TIPOS DE PLANCHA
GRANT ALL PRIVILEGES ON TABLE indicolors.plate_types TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.plate_types TO indicolors_app;

-- ============================================
-- 27. CREAR TABLA PRECIOS DE MONTAJE (formulario "Nuevo precio de montaje")
-- ============================================
CREATE TABLE indicolors.assembly_prices (
    assembly_price_id CHARACTER VARYING(64)  NOT NULL DEFAULT gen_random_uuid()::text,
    company_id         CHARACTER VARYING(64)  NOT NULL,
    name               CHARACTER VARYING(150) NOT NULL,
    cost               NUMERIC(12,2)           NOT NULL,
    state              BOOLEAN                 NOT NULL DEFAULT TRUE,
    creation_date      DATE                    NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT assembly_prices_pkey PRIMARY KEY (assembly_price_id),
    CONSTRAINT assembly_prices_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT assembly_prices_name_company_unique UNIQUE (company_id, name),
    CONSTRAINT assembly_prices_cost_check CHECK (cost >= 0)
);

-- 27.1 ÍNDICES PRECIOS DE MONTAJE
CREATE INDEX idx_assembly_prices_company_id ON indicolors.assembly_prices (company_id);
CREATE INDEX idx_assembly_prices_name ON indicolors.assembly_prices (name);
CREATE INDEX idx_assembly_prices_state ON indicolors.assembly_prices (state);
CREATE INDEX idx_assembly_prices_company_state ON indicolors.assembly_prices (company_id, state);

-- 27.2 COMENTARIOS PRECIOS DE MONTAJE
COMMENT ON TABLE indicolors.assembly_prices IS 'Catálogo de Precios de montaje (ej. Montaje estándar 4 tintas), usados al configurar órdenes de producción';
COMMENT ON COLUMN indicolors.assembly_prices.assembly_price_id IS 'Identificador único del precio de montaje';
COMMENT ON COLUMN indicolors.assembly_prices.company_id IS 'Identificador de la empresa dueña del precio de montaje';
COMMENT ON COLUMN indicolors.assembly_prices.name IS 'Nombre del precio de montaje (ej. Montaje estándar 4 tintas)';
COMMENT ON COLUMN indicolors.assembly_prices.cost IS 'Costo del montaje';
COMMENT ON COLUMN indicolors.assembly_prices.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.assembly_prices.creation_date IS 'Fecha de registro del precio de montaje en el sistema';

-- 27.3 PERMISOS PRECIOS DE MONTAJE
GRANT ALL PRIVILEGES ON TABLE indicolors.assembly_prices TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.assembly_prices TO indicolors_app;

-- ============================================
-- 28. CREAR TABLA TARIFAS POR MILLAR (formulario "Nueva tarifa por millar")
-- ============================================
CREATE TABLE indicolors.thousand_rates (
    thousand_rate_id     CHARACTER VARYING(64)  NOT NULL DEFAULT gen_random_uuid()::text,
    company_id           CHARACTER VARYING(64)  NOT NULL,
    name                 CHARACTER VARYING(150) NOT NULL,
    color_category       CHARACTER VARYING(50)  NOT NULL,
    thousand_unit        INTEGER                 NOT NULL DEFAULT 1000,
    price                NUMERIC(12,2)           NOT NULL,
    state                BOOLEAN                 NOT NULL DEFAULT TRUE,
    min_threshold_units  INTEGER                 NOT NULL,
    min_thousand         NUMERIC(10,2)           NOT NULL,
    decimal_threshold    NUMERIC(3,2)            NOT NULL,
    gripper_flip_price   NUMERIC(12,2),
    square_flip_price    NUMERIC(12,2),
    is_default           BOOLEAN                 NOT NULL DEFAULT FALSE,
    creation_date        DATE                    NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT thousand_rates_pkey PRIMARY KEY (thousand_rate_id),
    CONSTRAINT thousand_rates_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT thousand_rates_name_company_unique UNIQUE (company_id, name),
    CONSTRAINT thousand_rates_thousand_unit_check CHECK (thousand_unit > 0),
    CONSTRAINT thousand_rates_price_check CHECK (price >= 0),
    CONSTRAINT thousand_rates_min_threshold_units_check CHECK (min_threshold_units > 0),
    CONSTRAINT thousand_rates_min_thousand_check CHECK (min_thousand > 0),
    CONSTRAINT thousand_rates_decimal_threshold_check CHECK (decimal_threshold >= 0 AND decimal_threshold <= 1),
    CONSTRAINT thousand_rates_gripper_flip_price_check CHECK (gripper_flip_price IS NULL OR gripper_flip_price >= 0),
    CONSTRAINT thousand_rates_square_flip_price_check CHECK (square_flip_price IS NULL OR square_flip_price >= 0)
);

-- 28.1 ÍNDICES TARIFAS POR MILLAR
CREATE INDEX idx_thousand_rates_company_id ON indicolors.thousand_rates (company_id);
CREATE INDEX idx_thousand_rates_name ON indicolors.thousand_rates (name);
CREATE INDEX idx_thousand_rates_state ON indicolors.thousand_rates (state);
CREATE INDEX idx_thousand_rates_company_state ON indicolors.thousand_rates (company_id, state);
CREATE INDEX idx_thousand_rates_color_category ON indicolors.thousand_rates (color_category);
CREATE UNIQUE INDEX idx_thousand_rates_one_default_per_company_color
    ON indicolors.thousand_rates (company_id, lower(trim(color_category)))
    WHERE is_default = TRUE;

-- 28.2 COMENTARIOS TARIFAS POR MILLAR
COMMENT ON TABLE indicolors.thousand_rates IS 'Catálogo de Tarifas por millar (ej. Color básico), con reglas de millar y volteo, usadas al configurar órdenes de producción';
COMMENT ON COLUMN indicolors.thousand_rates.thousand_rate_id IS 'Identificador único de la tarifa por millar';
COMMENT ON COLUMN indicolors.thousand_rates.company_id IS 'Identificador de la empresa dueña de la tarifa';
COMMENT ON COLUMN indicolors.thousand_rates.name IS 'Nombre de la tarifa (ej. Color básico)';
COMMENT ON COLUMN indicolors.thousand_rates.color_category IS 'Categoría de color de la tarifa (ej. 1 COLOR, 2 COLORES)';
COMMENT ON COLUMN indicolors.thousand_rates.thousand_unit IS 'Unidad de millar sobre la que se calcula la tarifa (por defecto 1000)';
COMMENT ON COLUMN indicolors.thousand_rates.price IS 'Precio de la tarifa por millar';
COMMENT ON COLUMN indicolors.thousand_rates.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.thousand_rates.min_threshold_units IS 'Cantidad mínima en unidades para aplicar reglas de cobro por millar';
COMMENT ON COLUMN indicolors.thousand_rates.min_thousand IS 'Millar mínimo de venta asociado a la tarifa';
COMMENT ON COLUMN indicolors.thousand_rates.decimal_threshold IS 'Umbral decimal: si la parte decimal es mayor a este valor, sube al entero siguiente más cercano; en caso contrario, conserva solo la parte entera';
COMMENT ON COLUMN indicolors.thousand_rates.gripper_flip_price IS 'Precio por millar con volteo por pinza; NULL si no aplica';
COMMENT ON COLUMN indicolors.thousand_rates.square_flip_price IS 'Precio por millar con volteo por escuadra; NULL si no aplica';
COMMENT ON COLUMN indicolors.thousand_rates.is_default IS 'True=tarifa por defecto para la categoría/empresa; False=no';
COMMENT ON COLUMN indicolors.thousand_rates.creation_date IS 'Fecha de registro de la tarifa en el sistema';
COMMENT ON INDEX indicolors.idx_thousand_rates_one_default_per_company_color IS
    'Como máximo una tarifa por defecto por empresa y categoría de color';

-- 28.3 PERMISOS TARIFAS POR MILLAR
GRANT ALL PRIVILEGES ON TABLE indicolors.thousand_rates TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.thousand_rates TO indicolors_app;


-- ============================================
-- MÓDULO: ÓRDENES DE PRODUCCIÓN (production-orders)
-- Especificaciones + Preprensa + Corte de papel + Impresión + Terminados + Acabados + Cobro
-- ============================================

-- ============================================
-- 29. CREAR TABLA ÓRDENES DE PRODUCCIÓN (núcleo: Especificaciones + control)
-- ============================================
CREATE TABLE indicolors.production_orders (
    production_order_id              CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                       CHARACTER VARYING(64)       NOT NULL,
    order_number                     CHARACTER VARYING(20)       NOT NULL,
    version                          BIGINT                      NOT NULL DEFAULT 0,

    -- Especificaciones
    client_id                        CHARACTER VARYING(64)       NOT NULL,
    work_name                        CHARACTER VARYING(150)      NOT NULL,
    seller_id                        CHARACTER VARYING(64),
    order_date                       DATE                        NOT NULL DEFAULT CURRENT_DATE,
    requested_quantity               INTEGER                     NOT NULL,
    proposal_quantity_1              INTEGER,
    proposal_quantity_2              INTEGER,
    specifications_completed_at      TIMESTAMP WITHOUT TIME ZONE,

    -- Progreso de pasos que no tienen tabla 1:1 propia
    cutting_completed_at             TIMESTAMP WITHOUT TIME ZONE,
    printing_completed_at            TIMESTAMP WITHOUT TIME ZONE,
    finished_products_completed_at   TIMESTAMP WITHOUT TIME ZONE,
    finishing_processes_completed_at TIMESTAMP WITHOUT TIME ZONE,

    -- Corte de papel (globales del paso, pocos campos, se quedan en el núcleo)
    client_supplies_paper_default    BOOLEAN,
    rounding_margin                  INTEGER                     DEFAULT 2,

    status                           CHARACTER VARYING(30)       NOT NULL DEFAULT 'PENDING',
    state                            BOOLEAN                     NOT NULL DEFAULT TRUE,
    created_at                       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    created_by                       CHARACTER VARYING(64),
    updated_by                       CHARACTER VARYING(64),

    CONSTRAINT production_orders_pkey PRIMARY KEY (production_order_id),
    CONSTRAINT production_orders_order_number_company_unique UNIQUE (company_id, order_number),
    CONSTRAINT production_orders_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_orders_client_fk FOREIGN KEY (client_id) REFERENCES indicolors.clients (client_id),
    CONSTRAINT production_orders_seller_fk FOREIGN KEY (seller_id) REFERENCES indicolors.sellers (seller_id),
    CONSTRAINT production_orders_requested_quantity_check CHECK (requested_quantity > 0)
);

CREATE INDEX idx_production_orders_company_id ON indicolors.production_orders (company_id);
CREATE INDEX idx_production_orders_client_id ON indicolors.production_orders (client_id);
CREATE INDEX idx_production_orders_status ON indicolors.production_orders (status);
CREATE INDEX idx_production_orders_company_state ON indicolors.production_orders (company_id, state);
CREATE INDEX idx_production_orders_seller_id ON indicolors.production_orders (seller_id);

COMMENT ON TABLE indicolors.production_orders IS 'Núcleo de la Orden de Producción: Especificaciones y control de flujo. Preprensa y Cobro viven en tablas 1:1 separadas por tamaño (production_order_prepress_details, production_order_billing_details)';

COMMENT ON COLUMN indicolors.production_orders.production_order_id IS 'Identificador único de la Orden de Producción';
COMMENT ON COLUMN indicolors.production_orders.company_id IS 'Identificador de la empresa dueña de la Orden de Producción';
COMMENT ON COLUMN indicolors.production_orders.order_number IS 'Consecutivo corto de la OP por compañía (ej. OP-1, OP-42), generado por el backend; único por compañía';
COMMENT ON COLUMN indicolors.production_orders.version IS 'Control de concurrencia optimista';
COMMENT ON COLUMN indicolors.production_orders.client_id IS 'Identificador del cliente para el que se genera la Orden de Producción';
COMMENT ON COLUMN indicolors.production_orders.work_name IS 'Nombre del trabajo o pieza a producir';
COMMENT ON COLUMN indicolors.production_orders.seller_id IS 'Identificador del vendedor asociado a la Orden de Producción';
COMMENT ON COLUMN indicolors.production_orders.order_date IS 'Fecha de creación/registro de la Orden de Producción';
COMMENT ON COLUMN indicolors.production_orders.requested_quantity IS 'Cantidad solicitada por el cliente';
COMMENT ON COLUMN indicolors.production_orders.proposal_quantity_1 IS 'Cantidad alterna de propuesta 1 para comparativo de costeo (opcional)';
COMMENT ON COLUMN indicolors.production_orders.proposal_quantity_2 IS 'Cantidad alterna de propuesta 2 para comparativo de costeo (opcional)';
COMMENT ON COLUMN indicolors.production_orders.specifications_completed_at IS 'Fecha/hora en que se completó el paso de Especificaciones';
COMMENT ON COLUMN indicolors.production_orders.cutting_completed_at IS 'Fecha/hora en que se completó el paso de Corte de papel';
COMMENT ON COLUMN indicolors.production_orders.printing_completed_at IS 'Fecha/hora en que se completó el paso de Impresión';
COMMENT ON COLUMN indicolors.production_orders.finished_products_completed_at IS 'Fecha/hora en que se completó el paso de Terminados';
COMMENT ON COLUMN indicolors.production_orders.finishing_processes_completed_at IS 'Fecha/hora en que se completó el paso de Acabados';
COMMENT ON COLUMN indicolors.production_orders.client_supplies_paper_default IS 'Valor por defecto del paso Corte de papel: True=el cliente suministra el papel';
COMMENT ON COLUMN indicolors.production_orders.rounding_margin IS 'Margen de redondeo aplicado en los cálculos del paso de Corte de papel';
COMMENT ON COLUMN indicolors.production_orders.status IS 'Estado de la OP en planta: PENDING, IN_PROGRESS, etc.';
COMMENT ON COLUMN indicolors.production_orders.state IS 'True=Activa, False=Borrador eliminado (baja lógica)';
COMMENT ON COLUMN indicolors.production_orders.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_orders.updated_at IS 'Fecha y hora de la última actualización del registro';
COMMENT ON COLUMN indicolors.production_orders.created_by IS 'Identificador del usuario que creó la Orden de Producción';
COMMENT ON COLUMN indicolors.production_orders.updated_by IS 'Identificador del usuario que hizo la última actualización';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_orders TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_orders TO indicolors_app;

-- ============================================
-- 29.0 CREAR TABLA SECUENCIA DE NÚMEROS DE OP (consecutivo atómico por empresa)
-- ============================================
CREATE TABLE indicolors.production_order_number_sequences (
    company_id  CHARACTER VARYING(64) NOT NULL,
    last_value  BIGINT                NOT NULL,
    CONSTRAINT production_order_number_sequences_pkey PRIMARY KEY (company_id),
    CONSTRAINT production_order_number_sequences_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_number_sequences_last_value_check CHECK (last_value >= 0)
);

COMMENT ON TABLE indicolors.production_order_number_sequences IS
    'Último consecutivo de order_number emitido por compañía; se incrementa de forma atómica al crear una OP';
COMMENT ON COLUMN indicolors.production_order_number_sequences.company_id IS
    'Identificador de la empresa dueña del contador';
COMMENT ON COLUMN indicolors.production_order_number_sequences.last_value IS
    'Último número asignado (el order_number expuesto es OP-{last_value})';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_number_sequences TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_number_sequences TO indicolors_app;

-- ============================================
-- 29.1 CREAR TABLA DETALLE DE PREPRENSA (1:1 con production_orders)
-- ============================================
CREATE TABLE indicolors.production_order_prepress_details (
    production_order_id      CHARACTER VARYING(64)       NOT NULL,  -- mismo id que production_orders (1:1)
    company_id               CHARACTER VARYING(64)       NOT NULL,

    is_new_design            BOOLEAN,
    design_name              CHARACTER VARYING(150),
    existing_design_order_id CHARACTER VARYING(64),   -- self-FK a production_orders
    has_design_cost          BOOLEAN                     NOT NULL DEFAULT FALSE,
    design_cost              NUMERIC(12,2),
    client_supplies_plates   BOOLEAN,
    client_plate_type        CHARACTER VARYING(20),  -- client-supplies|existing-plate|new-plate
    new_plate_cost           NUMERIC(12,2),
    assembly_price_id        CHARACTER VARYING(64),
    assembly_price_name      CHARACTER VARYING(150),
    assembly_price_cost      NUMERIC(12,2),
    die_cut_line             BOOLEAN                     NOT NULL DEFAULT FALSE,
    uv_reserve               BOOLEAN                     NOT NULL DEFAULT FALSE,
    stamping                 BOOLEAN                     NOT NULL DEFAULT FALSE,
    embossing                BOOLEAN                     NOT NULL DEFAULT FALSE,
    total_plates_value       NUMERIC(12,2),
    prepress_discount_type   CHARACTER VARYING(10),  -- % | $
    prepress_discount_value  NUMERIC(12,2),
    prepress_completed_at    TIMESTAMP WITHOUT TIME ZONE,

    created_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_prepress_details_pkey PRIMARY KEY (production_order_id),
    CONSTRAINT production_order_prepress_details_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_prepress_details_order_fk FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_prepress_details_existing_design_order_fk FOREIGN KEY (existing_design_order_id) REFERENCES indicolors.production_orders (production_order_id),
    CONSTRAINT production_order_prepress_details_assembly_price_fk FOREIGN KEY (assembly_price_id) REFERENCES indicolors.assembly_prices (assembly_price_id),
    CONSTRAINT production_order_prepress_details_client_plate_type_check CHECK (client_plate_type IS NULL OR client_plate_type IN ('client-supplies','existing-plate','new-plate'))
);

CREATE INDEX idx_production_order_prepress_details_company_id ON indicolors.production_order_prepress_details (company_id);
CREATE INDEX idx_production_order_prepress_details_existing_design_order ON indicolors.production_order_prepress_details (existing_design_order_id);
CREATE INDEX idx_production_order_prepress_details_assembly_price_id ON indicolors.production_order_prepress_details (assembly_price_id);

COMMENT ON TABLE indicolors.production_order_prepress_details IS 'Detalle de Preprensa, relación 1:1 con production_orders (misma PK)';

COMMENT ON COLUMN indicolors.production_order_prepress_details.production_order_id IS 'Identificador de la Orden de Producción (mismo id que production_orders, relación 1:1)';
COMMENT ON COLUMN indicolors.production_order_prepress_details.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_prepress_details.is_new_design IS 'True=diseño nuevo, False=diseño existente reutilizado de otra OP';
COMMENT ON COLUMN indicolors.production_order_prepress_details.design_name IS 'Nombre del diseño, cuando is_new_design=true';
COMMENT ON COLUMN indicolors.production_order_prepress_details.existing_design_order_id IS 'Self-FK: OP origen del diseño reutilizado';
COMMENT ON COLUMN indicolors.production_order_prepress_details.has_design_cost IS 'True=el diseño tiene un costo adicional a cobrar';
COMMENT ON COLUMN indicolors.production_order_prepress_details.design_cost IS 'Costo del diseño, cuando has_design_cost=true';
COMMENT ON COLUMN indicolors.production_order_prepress_details.client_supplies_plates IS 'True=el cliente suministra las planchas';
COMMENT ON COLUMN indicolors.production_order_prepress_details.client_plate_type IS 'Origen de la plancha cuando el cliente no la suministra: client-supplies|existing-plate|new-plate';
COMMENT ON COLUMN indicolors.production_order_prepress_details.new_plate_cost IS 'Costo de la plancha nueva, cuando client_plate_type=new-plate';
COMMENT ON COLUMN indicolors.production_order_prepress_details.assembly_price_id IS 'Identificador de la tarifa de armado seleccionada (FK a assembly_prices)';
COMMENT ON COLUMN indicolors.production_order_prepress_details.assembly_price_name IS 'Snapshot del nombre de la tarifa de armado al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_prepress_details.assembly_price_cost IS 'Snapshot del costo de la tarifa de armado al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_prepress_details.die_cut_line IS 'True=incluye línea de corte/troquel';
COMMENT ON COLUMN indicolors.production_order_prepress_details.uv_reserve IS 'True=incluye Reserva UV';
COMMENT ON COLUMN indicolors.production_order_prepress_details.stamping IS 'True=incluye Estampado';
COMMENT ON COLUMN indicolors.production_order_prepress_details.embossing IS 'True=incluye Repujado/Relieve';
COMMENT ON COLUMN indicolors.production_order_prepress_details.total_plates_value IS 'Valor total de planchas de la Orden, calculado en servidor';
COMMENT ON COLUMN indicolors.production_order_prepress_details.prepress_discount_type IS 'Tipo de descuento de Preprensa: % | $';
COMMENT ON COLUMN indicolors.production_order_prepress_details.prepress_discount_value IS 'Valor del descuento de Preprensa';
COMMENT ON COLUMN indicolors.production_order_prepress_details.prepress_completed_at IS 'Fecha/hora en que se completó el paso de Preprensa';
COMMENT ON COLUMN indicolors.production_order_prepress_details.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_prepress_details.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_prepress_details TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_prepress_details TO indicolors_app;

-- ============================================
-- 29.2 CREAR TABLA DETALLE DE COBRO (1:1 con production_orders)
-- ============================================
CREATE TABLE indicolors.production_order_billing_details (
    production_order_id        CHARACTER VARYING(64)       NOT NULL,  -- mismo id que production_orders (1:1)
    company_id                 CHARACTER VARYING(64)       NOT NULL,

    billing_discount_type      CHARACTER VARYING(10),   -- descuento global
    billing_discount_value     NUMERIC(12,2),
    client_costing_mode        CHARACTER VARYING(10),  -- exact | volume
    client_discount_type       CHARACTER VARYING(12),  -- flujo exact
    client_discount_value      NUMERIC(12,2),
    client_profitability_type  CHARACTER VARYING(12),
    client_profitability_value NUMERIC(12,2),
    client_volume_costing      JSONB,                  -- flujo volume: 3 propuestas
    delivery_start_date        DATE,
    delivery_end_date          DATE,
    advance_percentage         NUMERIC(5,2)                DEFAULT 50,
    client_signature_name      CHARACTER VARYING(150),
    bank_account_id            CHARACTER VARYING(64),
    billing_completed_at       TIMESTAMP WITHOUT TIME ZONE,

    created_at                 TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_billing_details_pkey PRIMARY KEY (production_order_id),
    CONSTRAINT production_order_billing_details_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_billing_details_order_fk FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_billing_details_bank_account_fk FOREIGN KEY (bank_account_id) REFERENCES indicolors.bank_accounts (account_id),
    CONSTRAINT production_order_billing_details_client_costing_mode_check CHECK (client_costing_mode IS NULL OR client_costing_mode IN ('exact','volume'))
);

CREATE INDEX idx_production_order_billing_details_company_id ON indicolors.production_order_billing_details (company_id);
CREATE INDEX idx_production_order_billing_details_bank_account_id ON indicolors.production_order_billing_details (bank_account_id);

COMMENT ON TABLE indicolors.production_order_billing_details IS 'Detalle de Cobro, relación 1:1 con production_orders (misma PK)';

COMMENT ON COLUMN indicolors.production_order_billing_details.production_order_id IS 'Identificador de la Orden de Producción (mismo id que production_orders, relación 1:1)';
COMMENT ON COLUMN indicolors.production_order_billing_details.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_billing_details.billing_discount_type IS 'Tipo del descuento global de Cobro: % | $';
COMMENT ON COLUMN indicolors.production_order_billing_details.billing_discount_value IS 'Valor del descuento global de Cobro';
COMMENT ON COLUMN indicolors.production_order_billing_details.client_costing_mode IS 'Modo de costeo al cliente: exact | volume';
COMMENT ON COLUMN indicolors.production_order_billing_details.client_discount_type IS 'Tipo de descuento al cliente en el modo exact';
COMMENT ON COLUMN indicolors.production_order_billing_details.client_discount_value IS 'Valor de descuento al cliente en el modo exact';
COMMENT ON COLUMN indicolors.production_order_billing_details.client_profitability_type IS 'Tipo de rentabilidad al cliente en el modo exact';
COMMENT ON COLUMN indicolors.production_order_billing_details.client_profitability_value IS 'Valor de rentabilidad al cliente en el modo exact';
COMMENT ON COLUMN indicolors.production_order_billing_details.client_volume_costing IS 'JSONB con las 3 propuestas de cantidad/descuento/rentabilidad del flujo "volume"';
COMMENT ON COLUMN indicolors.production_order_billing_details.delivery_start_date IS 'Fecha inicial estimada de entrega';
COMMENT ON COLUMN indicolors.production_order_billing_details.delivery_end_date IS 'Fecha final estimada de entrega';
COMMENT ON COLUMN indicolors.production_order_billing_details.advance_percentage IS 'Porcentaje de anticipo solicitado al cliente (por defecto 50%)';
COMMENT ON COLUMN indicolors.production_order_billing_details.client_signature_name IS 'Nombre de la persona que firma/autoriza en representación del cliente';
COMMENT ON COLUMN indicolors.production_order_billing_details.bank_account_id IS 'Identificador de la cuenta bancaria usada para el cobro (FK a bank_accounts)';
COMMENT ON COLUMN indicolors.production_order_billing_details.billing_completed_at IS 'Fecha/hora en que se completó el paso de Cobro';
COMMENT ON COLUMN indicolors.production_order_billing_details.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_billing_details.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_billing_details TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_billing_details TO indicolors_app;

-- ============================================
-- 29.3 CREAR TABLA OPERADORES POR ETAPA (reemplaza 6 columnas repetidas del núcleo)
-- ============================================
CREATE TABLE indicolors.production_order_operators (
    production_order_operator_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                   CHARACTER VARYING(64)       NOT NULL,
    production_order_id          CHARACTER VARYING(64)       NOT NULL,
    stage                        CHARACTER VARYING(32)       NOT NULL,  -- PREPRESS|CUTTING|PRINTING|FINISHED_PRODUCTS|FINISHING_PROCESSES|BILLING
    user_id                      CHARACTER VARYING(64)       NOT NULL,
    role_code                    CHARACTER VARYING(64),

    created_at                   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_operators_pkey PRIMARY KEY (production_order_operator_id),
    CONSTRAINT production_order_operators_order_stage_unique UNIQUE (production_order_id, stage),
    CONSTRAINT production_order_operators_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_operators_order_fk FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_operators_user_fk FOREIGN KEY (user_id) REFERENCES indicolors.users (user_id),
    CONSTRAINT production_order_operators_stage_check CHECK (stage IN ('PREPRESS','CUTTING','PRINTING','FINISHED_PRODUCTS','FINISHING_PROCESSES','BILLING'))
);

CREATE INDEX idx_production_order_operators_company_id ON indicolors.production_order_operators (company_id);
CREATE INDEX idx_production_order_operators_order_id ON indicolors.production_order_operators (production_order_id);
CREATE INDEX idx_production_order_operators_user_id ON indicolors.production_order_operators (user_id);

COMMENT ON TABLE indicolors.production_order_operators IS 'Operador asignado por cada etapa de la OP; máximo 1 operador por etapa (UNIQUE order_id, stage)';

COMMENT ON COLUMN indicolors.production_order_operators.production_order_operator_id IS 'Identificador único del registro de operador por etapa';
COMMENT ON COLUMN indicolors.production_order_operators.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_operators.production_order_id IS 'Identificador de la Orden de Producción a la que pertenece el operador';
COMMENT ON COLUMN indicolors.production_order_operators.stage IS 'Etapa del wizard a la que corresponde el operador';
COMMENT ON COLUMN indicolors.production_order_operators.user_id IS 'Identificador del usuario/operador asignado a la etapa';
COMMENT ON COLUMN indicolors.production_order_operators.role_code IS 'Código de rol opcional del responsable (informativo; no obligatorio)';
COMMENT ON COLUMN indicolors.production_order_operators.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_operators.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_operators TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_operators TO indicolors_app;

-- ============================================
-- 29.4 CREAR TABLA DESCUENTOS POR ETAPA (reemplaza 6 columnas repetidas del núcleo)
-- ============================================
CREATE TABLE indicolors.production_order_stage_discounts (
    production_order_stage_discount_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                         CHARACTER VARYING(64)       NOT NULL,
    production_order_id                CHARACTER VARYING(64)       NOT NULL,
    stage                              CHARACTER VARYING(20)       NOT NULL,  -- CUTTING|FINISHED_PRODUCTS|FINISHING_PROCESSES (Preprensa y Cobro tienen su propio descuento en su tabla 1:1)
    discount_type                      CHARACTER VARYING(10),  -- % | $
    discount_value                     NUMERIC(12,2),

    created_at                         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_stage_discounts_pkey PRIMARY KEY (production_order_stage_discount_id),
    CONSTRAINT production_order_stage_discounts_order_stage_unique UNIQUE (production_order_id, stage),
    CONSTRAINT production_order_stage_discounts_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_stage_discounts_order_fk FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_stage_discounts_stage_check CHECK (stage IN ('CUTTING','FINISHED_PRODUCTS','FINISHING_PROCESSES')),
    CONSTRAINT production_order_stage_discounts_discount_type_check CHECK (discount_type IS NULL OR discount_type IN ('%','$'))
);

CREATE INDEX idx_production_order_stage_discounts_company_id ON indicolors.production_order_stage_discounts (company_id);
CREATE INDEX idx_production_order_stage_discounts_order_id ON indicolors.production_order_stage_discounts (production_order_id);

COMMENT ON TABLE indicolors.production_order_stage_discounts IS 'Descuento configurado por etapa (Corte, Terminados, Acabados); máximo 1 por etapa (UNIQUE order_id, stage). Preprensa y Cobro tienen su propio descuento dentro de production_order_prepress_details/billing_details por no repetirse';

COMMENT ON COLUMN indicolors.production_order_stage_discounts.production_order_stage_discount_id IS 'Identificador único del descuento por etapa';
COMMENT ON COLUMN indicolors.production_order_stage_discounts.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_stage_discounts.production_order_id IS 'Identificador de la Orden de Producción a la que pertenece el descuento';
COMMENT ON COLUMN indicolors.production_order_stage_discounts.stage IS 'Etapa a la que corresponde el descuento: CUTTING|FINISHED_PRODUCTS|FINISHING_PROCESSES';
COMMENT ON COLUMN indicolors.production_order_stage_discounts.discount_type IS 'Tipo de descuento de la etapa: % | $';
COMMENT ON COLUMN indicolors.production_order_stage_discounts.discount_value IS 'Valor del descuento de la etapa';
COMMENT ON COLUMN indicolors.production_order_stage_discounts.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_stage_discounts.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_stage_discounts TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_stage_discounts TO indicolors_app;

-- ============================================
-- 30. CREAR TABLA PLANCHAS DE LA ORDEN (Preprensa)
-- ============================================
CREATE TABLE indicolors.production_order_plates (
    production_order_plate_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                CHARACTER VARYING(64)       NOT NULL,
    production_order_id       CHARACTER VARYING(64)       NOT NULL,

    colors                    CHARACTER VARYING(20)       NOT NULL,
    plate_type_id             CHARACTER VARYING(64),
    plate_name                CHARACTER VARYING(80),
    plate_size                CHARACTER VARYING(30),
    plate_price               NUMERIC(12,2),

    quantity                  INTEGER                     NOT NULL,
    cavities                  SMALLINT,
    good_sizes                INTEGER,
    surplus                   INTEGER,
    plates_count              SMALLINT,
    total_value               NUMERIC(12,2),

    detail                    CHARACTER VARYING(100),
    observation               TEXT,
    manual_entry              BOOLEAN                     NOT NULL DEFAULT FALSE,
    plate_supply              CHARACTER VARYING(20),

    plate_replacement         BOOLEAN                     NOT NULL DEFAULT FALSE,
    replacement_quantity      INTEGER,

    created_at                TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_plates_pkey PRIMARY KEY (production_order_plate_id),
    CONSTRAINT production_order_plates_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_plates_order_fk FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_plates_plate_type_fk FOREIGN KEY (plate_type_id) REFERENCES indicolors.plate_types (plate_type_id),
    CONSTRAINT production_order_plates_quantity_check CHECK (quantity > 0)
);

CREATE INDEX idx_production_order_plates_company_id ON indicolors.production_order_plates (company_id);
CREATE INDEX idx_production_order_plates_order_id ON indicolors.production_order_plates (production_order_id);
CREATE INDEX idx_production_order_plates_plate_type_id ON indicolors.production_order_plates (plate_type_id);

COMMENT ON TABLE indicolors.production_order_plates IS 'Planchas configuradas en Preprensa, 0..N por Orden de Producción';

COMMENT ON COLUMN indicolors.production_order_plates.production_order_plate_id IS 'Identificador único de la plancha';
COMMENT ON COLUMN indicolors.production_order_plates.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_plates.production_order_id IS 'Identificador de la Orden de Producción a la que pertenece la plancha';
COMMENT ON COLUMN indicolors.production_order_plates.colors IS 'Cantidad/categoría de colores de la plancha (ej. 1 COLOR, 4 COLORES)';
COMMENT ON COLUMN indicolors.production_order_plates.plate_type_id IS 'Identificador del tipo de plancha seleccionado (FK a plate_types)';
COMMENT ON COLUMN indicolors.production_order_plates.plate_name IS 'Snapshot del nombre del tipo de plancha al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_plates.plate_size IS 'Snapshot de la medida del tipo de plancha al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_plates.plate_price IS 'Snapshot del precio del tipo de plancha al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_plates.quantity IS 'Cantidad solicitada para la plancha';
COMMENT ON COLUMN indicolors.production_order_plates.cavities IS 'Cantidad de cavidades del troquel/plancha';
COMMENT ON COLUMN indicolors.production_order_plates.good_sizes IS 'Calculado en servidor = quantity / cavities';
COMMENT ON COLUMN indicolors.production_order_plates.surplus IS 'Excedente calculado para la plancha';
COMMENT ON COLUMN indicolors.production_order_plates.plates_count IS 'Cantidad de planchas físicas requeridas';
COMMENT ON COLUMN indicolors.production_order_plates.total_value IS 'Calculado en servidor';
COMMENT ON COLUMN indicolors.production_order_plates.detail IS 'Detalle adicional de la plancha';
COMMENT ON COLUMN indicolors.production_order_plates.observation IS 'Observaciones libres sobre la plancha';
COMMENT ON COLUMN indicolors.production_order_plates.manual_entry IS 'True=los valores de la plancha se ingresaron manualmente, no desde catálogo';
COMMENT ON COLUMN indicolors.production_order_plates.plate_supply IS 'Origen de suministro de la plancha';
COMMENT ON COLUMN indicolors.production_order_plates.plate_replacement IS 'True=esta plancha es una reposición';
COMMENT ON COLUMN indicolors.production_order_plates.replacement_quantity IS 'Cantidad de reposición, cuando plate_replacement=true';
COMMENT ON COLUMN indicolors.production_order_plates.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_plates.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_plates TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_plates TO indicolors_app;

-- ============================================
-- 31. CREAR TABLA CORTE DE PAPEL DE LA ORDEN
-- ============================================
CREATE TABLE indicolors.production_order_paper_rows (
    production_order_paper_row_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                    CHARACTER VARYING(64)       NOT NULL,
    production_order_id           CHARACTER VARYING(64)       NOT NULL,
    plate_id                      CHARACTER VARYING(64)       NOT NULL,
    parent_row_id                 CHARACTER VARYING(64),  -- self-FK (fila de faltante)

    cut_row_key                   CHARACTER VARYING(50)       NOT NULL,
    is_missing_supply             BOOLEAN                     NOT NULL DEFAULT FALSE,
    missing_sheets_quantity       INTEGER,

    client_supplies_paper         BOOLEAN                     NOT NULL,

    paper_type_id                 CHARACTER VARYING(64),
    supplier_id                   CHARACTER VARYING(64),
    paper_name                    CHARACTER VARYING(80),
    paper_size                    CHARACTER VARYING(30),
    sheet_value                   NUMERIC(12,2),
    package_unit                  INTEGER,
    is_coated                     BOOLEAN,

    cut_layout_id                 CHARACTER VARYING(64),
    cut_layout_name               CHARACTER VARYING(80),
    cut_layout_size               CHARACTER VARYING(30),
    pieces_per_sheet              SMALLINT,
    cut_value                     NUMERIC(12,2),

    is_paper_cut                  BOOLEAN,
    delivered_sheets_by_client    INTEGER,
    manual_good_sizes             INTEGER,
    manual_surplus                INTEGER,

    calculated_sheets_count       INTEGER,
    total_paper_value             NUMERIC(12,2),
    total_cut_value               NUMERIC(12,2),

    created_at                    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_paper_rows_pkey PRIMARY KEY (production_order_paper_row_id),
    CONSTRAINT production_order_paper_rows_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_paper_rows_order_fk FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_paper_rows_plate_fk FOREIGN KEY (plate_id) REFERENCES indicolors.production_order_plates (production_order_plate_id),
    CONSTRAINT production_order_paper_rows_parent_row_fk FOREIGN KEY (parent_row_id) REFERENCES indicolors.production_order_paper_rows (production_order_paper_row_id),
    CONSTRAINT production_order_paper_rows_paper_type_fk FOREIGN KEY (paper_type_id) REFERENCES indicolors.paper_types (paper_type_id),
    CONSTRAINT production_order_paper_rows_supplier_fk FOREIGN KEY (supplier_id) REFERENCES indicolors.suppliers (supplier_id) ON DELETE SET NULL,
    CONSTRAINT production_order_paper_rows_cut_layout_fk FOREIGN KEY (cut_layout_id) REFERENCES indicolors.cut_layouts (cut_layout_id)
);

CREATE INDEX idx_production_order_paper_rows_company_id ON indicolors.production_order_paper_rows (company_id);
CREATE INDEX idx_production_order_paper_rows_order_id ON indicolors.production_order_paper_rows (production_order_id);
CREATE INDEX idx_production_order_paper_rows_plate_id ON indicolors.production_order_paper_rows (plate_id);
CREATE INDEX idx_production_order_paper_rows_parent_row_id ON indicolors.production_order_paper_rows (parent_row_id);
CREATE INDEX idx_production_order_paper_rows_cut_row_key ON indicolors.production_order_paper_rows (cut_row_key);
CREATE INDEX idx_production_order_paper_rows_paper_type_id ON indicolors.production_order_paper_rows (paper_type_id);
CREATE INDEX idx_production_order_paper_rows_supplier_id ON indicolors.production_order_paper_rows (supplier_id);
CREATE INDEX idx_production_order_paper_rows_cut_layout_id ON indicolors.production_order_paper_rows (cut_layout_id);

COMMENT ON TABLE indicolors.production_order_paper_rows IS 'Corte de papel por plancha, 0..N filas (incluye filas de faltante cubierto por litografía)';

COMMENT ON COLUMN indicolors.production_order_paper_rows.production_order_paper_row_id IS 'Identificador único de la fila de corte de papel';
COMMENT ON COLUMN indicolors.production_order_paper_rows.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_paper_rows.production_order_id IS 'Identificador de la Orden de Producción a la que pertenece la fila';
COMMENT ON COLUMN indicolors.production_order_paper_rows.plate_id IS 'Identificador de la plancha a la que pertenece la fila (FK a production_order_plates)';
COMMENT ON COLUMN indicolors.production_order_paper_rows.parent_row_id IS 'Self-FK: fila de corte original cuando esta fila es un faltante cubierto por litografía';
COMMENT ON COLUMN indicolors.production_order_paper_rows.cut_row_key IS 'Clave de agrupación/visualización del frontend para identificar filas de corte de una misma plancha (incluye faltantes). NO es FK ni se referencia desde production_order_postpress_records: Terminados y Acabados se vinculan a nivel de plancha vía plate_id, no por fila de corte individual';
COMMENT ON COLUMN indicolors.production_order_paper_rows.is_missing_supply IS 'True=esta fila representa un faltante de papel cubierto por litografía';
COMMENT ON COLUMN indicolors.production_order_paper_rows.missing_sheets_quantity IS 'Cantidad de pliegos faltantes cubiertos, cuando is_missing_supply=true';
COMMENT ON COLUMN indicolors.production_order_paper_rows.client_supplies_paper IS 'True=el cliente suministra el papel de esta fila';
COMMENT ON COLUMN indicolors.production_order_paper_rows.paper_type_id IS 'Identificador del tipo de papel seleccionado (FK a paper_types)';
COMMENT ON COLUMN indicolors.production_order_paper_rows.supplier_id IS 'Proveedor del tipo de papel usado para valor hoja y unidad empaque en este corte';
COMMENT ON COLUMN indicolors.production_order_paper_rows.paper_name IS 'Snapshot del nombre del tipo de papel al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_paper_rows.paper_size IS 'Snapshot de la medida del tipo de papel al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_paper_rows.sheet_value IS 'Snapshot del valor del pliego al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_paper_rows.package_unit IS 'Snapshot de la unidad de empaque del tipo de papel';
COMMENT ON COLUMN indicolors.production_order_paper_rows.is_coated IS 'Snapshot de si el papel es estucado/recubierto';
COMMENT ON COLUMN indicolors.production_order_paper_rows.cut_layout_id IS 'Identificador del patrón de corte seleccionado (FK a cut_layouts)';
COMMENT ON COLUMN indicolors.production_order_paper_rows.cut_layout_name IS 'Snapshot del nombre del patrón de corte al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_paper_rows.cut_layout_size IS 'Snapshot de la medida del patrón de corte al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_paper_rows.pieces_per_sheet IS 'Snapshot de piezas por pliego del patrón de corte';
COMMENT ON COLUMN indicolors.production_order_paper_rows.cut_value IS 'Snapshot del valor de corte al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_paper_rows.is_paper_cut IS 'Cuando el cliente suministra el papel: True=el papel ya viene cortado';
COMMENT ON COLUMN indicolors.production_order_paper_rows.delivered_sheets_by_client IS 'Cantidad de pliegos entregados por el cliente, cuando is_paper_cut=true';
COMMENT ON COLUMN indicolors.production_order_paper_rows.manual_good_sizes IS 'Cantidad de piezas buenas ingresada manualmente, cuando is_paper_cut=false';
COMMENT ON COLUMN indicolors.production_order_paper_rows.manual_surplus IS 'Excedente ingresado manualmente, cuando is_paper_cut=false';
COMMENT ON COLUMN indicolors.production_order_paper_rows.calculated_sheets_count IS 'Cantidad de pliegos calculada en servidor';
COMMENT ON COLUMN indicolors.production_order_paper_rows.total_paper_value IS 'Valor total de papel, calculado en servidor';
COMMENT ON COLUMN indicolors.production_order_paper_rows.total_cut_value IS 'Valor total de corte, calculado en servidor';
COMMENT ON COLUMN indicolors.production_order_paper_rows.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_paper_rows.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_paper_rows TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_paper_rows TO indicolors_app;

-- ============================================
-- 32. CREAR TABLA IMPRESIÓN DE LA ORDEN (1 por plancha)
-- ============================================
CREATE TABLE indicolors.production_order_print (
    production_order_print_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                CHARACTER VARYING(64)       NOT NULL,
    production_order_id       CHARACTER VARYING(64)       NOT NULL,
    plate_id                  CHARACTER VARYING(64)       NOT NULL,

    client_supplies_sherpa    BOOLEAN,
    sherpa_test_price         NUMERIC(12,2),
    machine_output_value      NUMERIC(12,2),
    ink_estimation            JSONB,

    printing_discount_type    CHARACTER VARYING(10),
    printing_discount_value   NUMERIC(12,2),
    completed                 BOOLEAN                     NOT NULL DEFAULT FALSE,

    created_at                TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_print_pkey PRIMARY KEY (production_order_print_id),
    CONSTRAINT production_order_print_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_print_order_fk FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_print_plate_fk FOREIGN KEY (plate_id) REFERENCES indicolors.production_order_plates (production_order_plate_id),
    CONSTRAINT production_order_print_plate_unique UNIQUE (plate_id)
);

CREATE INDEX idx_production_order_print_company_id ON indicolors.production_order_print (company_id);
CREATE INDEX idx_production_order_print_order_id ON indicolors.production_order_print (production_order_id);
CREATE INDEX idx_prints_ink_estimation_gin ON indicolors.production_order_print USING gin (ink_estimation jsonb_path_ops);

COMMENT ON TABLE indicolors.production_order_print IS 'Configuración de impresión por plancha; máximo 1 registro por plancha (UNIQUE plate_id)';

COMMENT ON COLUMN indicolors.production_order_print.production_order_print_id IS 'Identificador único de la configuración de impresión';
COMMENT ON COLUMN indicolors.production_order_print.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_print.production_order_id IS 'Identificador de la Orden de Producción a la que pertenece';
COMMENT ON COLUMN indicolors.production_order_print.plate_id IS 'Identificador de la plancha a la que pertenece (FK, único por plancha)';
COMMENT ON COLUMN indicolors.production_order_print.client_supplies_sherpa IS 'True=el cliente suministra la prueba Sherpa';
COMMENT ON COLUMN indicolors.production_order_print.sherpa_test_price IS 'Precio de la prueba Sherpa, cuando client_supplies_sherpa=false';
COMMENT ON COLUMN indicolors.production_order_print.machine_output_value IS 'Valor de salida de máquina';
COMMENT ON COLUMN indicolors.production_order_print.ink_estimation IS 'JSONB: metadatos numéricos de estimación de tintas + objectKey/previewObjectKey (sin Base64/data-URL)';
COMMENT ON COLUMN indicolors.production_order_print.printing_discount_type IS 'Tipo de descuento de Impresión para esta plancha: % | $';
COMMENT ON COLUMN indicolors.production_order_print.printing_discount_value IS 'Valor del descuento de Impresión para esta plancha';
COMMENT ON COLUMN indicolors.production_order_print.completed IS 'True=el paso de Impresión para esta plancha está finalizado';
COMMENT ON COLUMN indicolors.production_order_print.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_print.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_print TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_print TO indicolors_app;

-- ============================================
-- 33. CREAR TABLA ENTRADAS TIRO/RETIRO DE IMPRESIÓN
-- ============================================
CREATE TABLE indicolors.production_order_print_entries (
    production_order_print_entry_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                      CHARACTER VARYING(64)       NOT NULL,
    print_id                        CHARACTER VARYING(64)       NOT NULL,

    shots_ink_count                 SMALLINT                    NOT NULL,
    shots_inks                      JSONB                       NOT NULL,
    reverse_ink_count               SMALLINT                    NOT NULL,
    reverse_inks                    JSONB                       NOT NULL,

    -- Grupo Color básico
    basic_flip_type                 CHARACTER VARYING(20)       NOT NULL,  -- no-flip|gripper-flip|square-flip
    basic_thousand_rate_id          CHARACTER VARYING(64),
    basic_rate_name                 CHARACTER VARYING(150),
    basic_rate_price                NUMERIC(12,2),
    basic_rate_gripper_flip_price   NUMERIC(12,2),
    basic_rate_square_flip_price    NUMERIC(12,2),
    basic_calculated_thousands      NUMERIC(10,2),
    basic_printing_price            NUMERIC(12,2),

    -- Grupo Pantone
    pantone_flip_type               CHARACTER VARYING(20)       NOT NULL,
    client_supplies_pantone_ink     BOOLEAN,
    pantone_ink_charge_price        NUMERIC(12,2),
    pantone_thousand_rate_id        CHARACTER VARYING(64),
    pantone_rate_name               CHARACTER VARYING(150),
    pantone_rate_price              NUMERIC(12,2),
    pantone_rate_gripper_flip_price NUMERIC(12,2),
    pantone_rate_square_flip_price  NUMERIC(12,2),
    pantone_calculated_thousands    NUMERIC(10,2),
    pantone_printing_price          NUMERIC(12,2),

    created_at                      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_print_entries_pkey PRIMARY KEY (production_order_print_entry_id),
    CONSTRAINT production_order_print_entries_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_print_entries_print_fk FOREIGN KEY (print_id) REFERENCES indicolors.production_order_print (production_order_print_id) ON DELETE CASCADE,
    CONSTRAINT production_order_print_entries_basic_rate_fk FOREIGN KEY (basic_thousand_rate_id) REFERENCES indicolors.thousand_rates (thousand_rate_id),
    CONSTRAINT production_order_print_entries_pantone_rate_fk FOREIGN KEY (pantone_thousand_rate_id) REFERENCES indicolors.thousand_rates (thousand_rate_id),
    CONSTRAINT production_order_print_entries_basic_flip_check CHECK (basic_flip_type IN ('no-flip','gripper-flip','square-flip')),
    CONSTRAINT production_order_print_entries_pantone_flip_check CHECK (pantone_flip_type IN ('no-flip','gripper-flip','square-flip'))
);

CREATE INDEX idx_production_order_print_entries_company_id ON indicolors.production_order_print_entries (company_id);
CREATE INDEX idx_production_order_print_entries_print_id ON indicolors.production_order_print_entries (print_id);
CREATE INDEX idx_production_order_print_entries_basic_thousand_rate_id ON indicolors.production_order_print_entries (basic_thousand_rate_id);
CREATE INDEX idx_production_order_print_entries_pantone_thousand_rate_id ON indicolors.production_order_print_entries (pantone_thousand_rate_id);

COMMENT ON TABLE indicolors.production_order_print_entries IS 'Entradas de tiro/retiro por registro de impresión, 0..N por plancha';

COMMENT ON COLUMN indicolors.production_order_print_entries.production_order_print_entry_id IS 'Identificador único de la entrada de tiro/retiro';
COMMENT ON COLUMN indicolors.production_order_print_entries.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_print_entries.print_id IS 'Identificador del registro de impresión al que pertenece la entrada (FK a production_order_print)';
COMMENT ON COLUMN indicolors.production_order_print_entries.shots_ink_count IS 'Cantidad de tintas usadas en el tiro (frente)';
COMMENT ON COLUMN indicolors.production_order_print_entries.shots_inks IS 'JSONB: lista de tintas usadas en el tiro (frente)';
COMMENT ON COLUMN indicolors.production_order_print_entries.reverse_ink_count IS 'Cantidad de tintas usadas en el retiro (reverso)';
COMMENT ON COLUMN indicolors.production_order_print_entries.reverse_inks IS 'JSONB: lista de tintas usadas en el retiro (reverso)';
COMMENT ON COLUMN indicolors.production_order_print_entries.basic_flip_type IS 'Tipo de volteo del grupo Color básico: no-flip|gripper-flip|square-flip';
COMMENT ON COLUMN indicolors.production_order_print_entries.basic_thousand_rate_id IS 'Identificador de la tarifa por millar del grupo básico (FK a thousand_rates)';
COMMENT ON COLUMN indicolors.production_order_print_entries.basic_rate_name IS 'Snapshot del nombre de la tarifa del grupo básico al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_print_entries.basic_rate_price IS 'Snapshot del precio de la tarifa del grupo básico al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_print_entries.basic_rate_gripper_flip_price IS 'Snapshot de thousand_rates.gripper_flip_price al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_print_entries.basic_rate_square_flip_price IS 'Snapshot de thousand_rates.square_flip_price al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_print_entries.basic_calculated_thousands IS 'Millares calculados para el grupo básico, calculado en servidor';
COMMENT ON COLUMN indicolors.production_order_print_entries.basic_printing_price IS 'Precio de impresión del grupo básico, calculado en servidor';
COMMENT ON COLUMN indicolors.production_order_print_entries.pantone_flip_type IS 'Tipo de volteo del grupo Pantone: no-flip|gripper-flip|square-flip';
COMMENT ON COLUMN indicolors.production_order_print_entries.client_supplies_pantone_ink IS 'Decisión de suministro de tinta Pantone, capturada por entrada (no por plancha)';
COMMENT ON COLUMN indicolors.production_order_print_entries.pantone_ink_charge_price IS 'Precio cobrado por tinta Pantone, cuando el cliente no la suministra';
COMMENT ON COLUMN indicolors.production_order_print_entries.pantone_thousand_rate_id IS 'Identificador de la tarifa por millar del grupo Pantone (FK a thousand_rates)';
COMMENT ON COLUMN indicolors.production_order_print_entries.pantone_rate_name IS 'Snapshot del nombre de la tarifa del grupo Pantone al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_print_entries.pantone_rate_price IS 'Snapshot del precio de la tarifa del grupo Pantone al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_print_entries.pantone_rate_gripper_flip_price IS 'Snapshot de thousand_rates.gripper_flip_price para el grupo Pantone';
COMMENT ON COLUMN indicolors.production_order_print_entries.pantone_rate_square_flip_price IS 'Snapshot de thousand_rates.square_flip_price para el grupo Pantone';
COMMENT ON COLUMN indicolors.production_order_print_entries.pantone_calculated_thousands IS 'Millares calculados para el grupo Pantone, calculado en servidor';
COMMENT ON COLUMN indicolors.production_order_print_entries.pantone_printing_price IS 'Precio de impresión del grupo Pantone, calculado en servidor';
COMMENT ON COLUMN indicolors.production_order_print_entries.created_at IS 'Fecha y hora de creación del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_print_entries TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_print_entries TO indicolors_app;

-- ============================================
-- 34. CREAR TABLA REGISTROS DE TERMINADOS/ACABADOS (1 por plancha x tipo)
-- ============================================
CREATE TABLE indicolors.production_order_postpress_records (
    production_order_postpress_record_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                           CHARACTER VARYING(64)       NOT NULL,
    production_order_id                  CHARACTER VARYING(64)       NOT NULL,
    plate_id                             CHARACTER VARYING(64)       NOT NULL,

    type                                 CHARACTER VARYING(20)       NOT NULL, -- FINISHED_PRODUCT | FINISHING_PROCESS
    completed                            BOOLEAN                     NOT NULL DEFAULT FALSE,

    created_at                           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_postpress_records_pkey PRIMARY KEY (production_order_postpress_record_id),
    CONSTRAINT production_order_postpress_records_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_postpress_records_order_fk FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_postpress_records_plate_fk FOREIGN KEY (plate_id) REFERENCES indicolors.production_order_plates (production_order_plate_id),
    CONSTRAINT production_order_postpress_records_type_check CHECK (type IN ('FINISHED_PRODUCT','FINISHING_PROCESS')),
    CONSTRAINT production_order_postpress_records_plate_type_unique UNIQUE (plate_id, type)
);

CREATE INDEX idx_production_order_postpress_records_company_id ON indicolors.production_order_postpress_records (company_id);
CREATE INDEX idx_production_order_postpress_records_order_id ON indicolors.production_order_postpress_records (production_order_id);
CREATE INDEX idx_production_order_postpress_records_type ON indicolors.production_order_postpress_records (type);

COMMENT ON TABLE indicolors.production_order_postpress_records IS 'Registro de Terminados o Acabados por plancha, discriminado por type; máximo 1 por plancha y tipo';

COMMENT ON COLUMN indicolors.production_order_postpress_records.production_order_postpress_record_id IS 'Identificador único del registro de Terminados/Acabados';
COMMENT ON COLUMN indicolors.production_order_postpress_records.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_postpress_records.production_order_id IS 'Identificador de la Orden de Producción a la que pertenece';
COMMENT ON COLUMN indicolors.production_order_postpress_records.plate_id IS 'Identificador de la plancha a la que pertenece el registro (FK a production_order_plates)';
COMMENT ON COLUMN indicolors.production_order_postpress_records.type IS 'Tipo de registro: FINISHED_PRODUCT (Terminados) | FINISHING_PROCESS (Acabados)';
COMMENT ON COLUMN indicolors.production_order_postpress_records.completed IS 'True=el registro de Terminados/Acabados para esta plancha está finalizado';
COMMENT ON COLUMN indicolors.production_order_postpress_records.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_postpress_records.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_postpress_records TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_postpress_records TO indicolors_app;

-- ============================================
-- 35. CREAR TABLA LÍNEAS DE TERMINADOS/ACABADOS
-- ============================================
CREATE TABLE indicolors.production_order_postpress_lines (
    production_order_postpress_line_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                         CHARACTER VARYING(64)       NOT NULL,
    record_id                          CHARACTER VARYING(64)       NOT NULL,

    catalog_item_id                    CHARACTER VARYING(64),  -- FK lógica a finished_products o finishing_processes según el type del record padre
    item_name                          CHARACTER VARYING(150),
    source                             CHARACTER VARYING(15)       NOT NULL,  -- catalog | quick-access

    value_per_cm2                      NUMERIC(12,4),
    min_cost                           NUMERIC(12,2),
    area_factor                        NUMERIC(10,4),
    good_sizes                         INTEGER,
    calculated_price                   NUMERIC(12,2),
    charged_price                      NUMERIC(12,2),
    applied_min_cost                   BOOLEAN                     NOT NULL DEFAULT FALSE,

    positive                           BOOLEAN,  -- solo type=FINISHED_PRODUCT (Reserva UV)
    cliche                             BOOLEAN,  -- solo type=FINISHED_PRODUCT (Estampado)

    created_at                         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_postpress_lines_pkey PRIMARY KEY (production_order_postpress_line_id),
    CONSTRAINT production_order_postpress_lines_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_postpress_lines_record_fk FOREIGN KEY (record_id) REFERENCES indicolors.production_order_postpress_records (production_order_postpress_record_id) ON DELETE CASCADE,
    CONSTRAINT production_order_postpress_lines_source_check CHECK (source IN ('catalog','quick-access'))
);

CREATE INDEX idx_production_order_postpress_lines_company_id ON indicolors.production_order_postpress_lines (company_id);
CREATE INDEX idx_production_order_postpress_lines_record_id ON indicolors.production_order_postpress_lines (record_id);
CREATE INDEX idx_production_order_postpress_lines_catalog_item_id ON indicolors.production_order_postpress_lines (catalog_item_id);

COMMENT ON TABLE indicolors.production_order_postpress_lines IS 'Líneas de costeo confirmadas dentro de un registro de Terminados/Acabados, 0..N por registro';

COMMENT ON COLUMN indicolors.production_order_postpress_lines.production_order_postpress_line_id IS 'Identificador único de la línea de costeo';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.record_id IS 'Identificador del registro de Terminados/Acabados al que pertenece la línea (FK a production_order_postpress_records)';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.catalog_item_id IS 'Referencia lógica a finished_products.finished_product_id o finishing_processes.finishing_process_id, según el type del registro padre; validar en el Service, no hay FK física por ser polimórfico';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.item_name IS 'Snapshot del nombre del ítem de catálogo al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.source IS 'Origen de selección del ítem: catalog (select completo) | quick-access (acceso rápido)';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.value_per_cm2 IS 'Snapshot del valor por cm² del ítem de catálogo';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.min_cost IS 'Snapshot del costo mínimo del ítem de catálogo';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.area_factor IS 'Factor de área usado en el cálculo del precio';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.good_sizes IS 'Cantidad de piezas buenas sobre las que se calcula el costeo';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.calculated_price IS 'Precio calculado en servidor antes de aplicar el costo mínimo';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.charged_price IS 'Precio finalmente cobrado (igual a calculated_price o a min_cost si aplica)';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.applied_min_cost IS 'True si se cobró el costo mínimo del catálogo en vez del precio calculado';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.positive IS 'Solo aplica cuando el registro padre es type=FINISHED_PRODUCT y el ítem es Reserva UV';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.cliche IS 'Solo aplica cuando el registro padre es type=FINISHED_PRODUCT y el ítem es Estampado';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.created_at IS 'Fecha y hora de creación del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_postpress_lines TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_postpress_lines TO indicolors_app;
