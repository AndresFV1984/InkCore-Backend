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
    sheet_value     NUMERIC(12,2)           NOT NULL,
    package_unit    INTEGER                 NOT NULL,
    is_coated       BOOLEAN                 NOT NULL DEFAULT FALSE,
    state           BOOLEAN                 NOT NULL DEFAULT TRUE,
    creation_date   DATE                    NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT paper_types_pkey PRIMARY KEY (paper_type_id),
    CONSTRAINT paper_types_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT paper_types_name_company_unique UNIQUE (company_id, name),
    CONSTRAINT paper_types_width_check CHECK (width > 0),
    CONSTRAINT paper_types_height_check CHECK (height > 0),
    CONSTRAINT paper_types_unit_check CHECK (unit IN ('cm', 'mm', 'in')),
    CONSTRAINT paper_types_sheet_value_check CHECK (sheet_value >= 0),
    CONSTRAINT paper_types_package_unit_check CHECK (package_unit > 0)
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
COMMENT ON COLUMN indicolors.paper_types.sheet_value IS 'Valor de la hoja/pliego';
COMMENT ON COLUMN indicolors.paper_types.package_unit IS 'Cantidad de hojas por unidad de empaque';
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

