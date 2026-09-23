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
    credit_days    INTEGER                NOT NULL DEFAULT 0,
    state          BOOLEAN                NOT NULL DEFAULT TRUE,
    creation_date  DATE                   NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT clients_pkey PRIMARY KEY (client_id),
    CONSTRAINT clients_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT clients_document_type_check
        CHECK (document_type IS NULL OR document_type IN ('CC', 'CE', 'TI', 'PA', 'NIT')),
    CONSTRAINT clients_email_check
        CHECK (email IS NULL OR email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT clients_credit_days_check
        CHECK (credit_days >= 0)
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
COMMENT ON COLUMN indicolors.clients.credit_days IS
    'Días de crédito (Net N) para calcular accounts_receivable.due_date al abrir la CxC; 0=contado';
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
-- 22.4 CREAR TABLA HISTORIAL DE CONVERSIONES RGB → CMYK
-- ============================================
CREATE TABLE indicolors.conversion_history (
    conversion_history_id CHARACTER VARYING(64)  NOT NULL,
    original_file_name    CHARACTER VARYING(255) NOT NULL,
    original_size_bytes   BIGINT                 NOT NULL,
    final_size_bytes      BIGINT                 NOT NULL,
    rendering_intent      CHARACTER VARYING(40)  NOT NULL,
    icc_profile_used      CHARACTER VARYING(120),
    conversion_date       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    duration_ms           BIGINT                 NOT NULL,
    user_id               CHARACTER VARYING(64),
    mime_type             CHARACTER VARYING(120),
    CONSTRAINT conversion_history_pkey PRIMARY KEY (conversion_history_id),
    CONSTRAINT conversion_history_sizes_check
        CHECK (original_size_bytes >= 0 AND final_size_bytes >= 0),
    CONSTRAINT conversion_history_duration_check
        CHECK (duration_ms >= 0),
    CONSTRAINT conversion_history_intent_check
        CHECK (rendering_intent IN ('PERCEPTUAL', 'RELATIVE_COLORIMETRIC'))
);

CREATE INDEX idx_conversion_history_user_id ON indicolors.conversion_history (user_id);
CREATE INDEX idx_conversion_history_conversion_date ON indicolors.conversion_history (conversion_date DESC);
CREATE INDEX idx_conversion_history_intent ON indicolors.conversion_history (rendering_intent);

COMMENT ON TABLE indicolors.conversion_history IS 'Metadatos de conversiones de color RGB→CMYK (el binario no se persiste)';
COMMENT ON COLUMN indicolors.conversion_history.conversion_history_id IS 'Identificador único de la conversión';
COMMENT ON COLUMN indicolors.conversion_history.original_file_name IS 'Nombre del archivo original subido';
COMMENT ON COLUMN indicolors.conversion_history.original_size_bytes IS 'Tamaño en bytes del archivo de entrada';
COMMENT ON COLUMN indicolors.conversion_history.final_size_bytes IS 'Tamaño en bytes del archivo CMYK (suele ser mayor por 4 canales)';
COMMENT ON COLUMN indicolors.conversion_history.rendering_intent IS 'Intent ICC: PERCEPTUAL o RELATIVE_COLORIMETRIC';
COMMENT ON COLUMN indicolors.conversion_history.icc_profile_used IS 'Nombre del perfil ICC de destino usado';
COMMENT ON COLUMN indicolors.conversion_history.conversion_date IS 'Fecha/hora UTC de la conversión';
COMMENT ON COLUMN indicolors.conversion_history.duration_ms IS 'Duración del procesamiento en milisegundos';
COMMENT ON COLUMN indicolors.conversion_history.user_id IS 'Usuario autenticado (subject del JWT), si aplica';
COMMENT ON COLUMN indicolors.conversion_history.mime_type IS 'MIME type declarado en la petición';

GRANT ALL PRIVILEGES ON TABLE indicolors.conversion_history TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.conversion_history TO indicolors_app;

-- ============================================
-- 22.5 CREAR TABLA HISTORIAL DE ESTIMACIONES DE TINTA
-- ============================================
CREATE TABLE indicolors.ink_estimate_history (
    ink_estimate_history_id CHARACTER VARYING(64)   NOT NULL,
    original_file_name      CHARACTER VARYING(255) NOT NULL,
    original_size_bytes     BIGINT                  NOT NULL,
    mime_type               CHARACTER VARYING(120),
    width_cm                NUMERIC(12, 4)          NOT NULL,
    height_cm               NUMERIC(12, 4)          NOT NULL,
    sheet_count             INTEGER                 NOT NULL,
    dpi                     INTEGER                 NOT NULL,
    grams_per_cm2           NUMERIC(16, 10)         NOT NULL,
    process_grams_order     NUMERIC(18, 6)          NOT NULL,
    spot_grams_order        NUMERIC(18, 6)          NOT NULL,
    total_grams_order       NUMERIC(18, 6)          NOT NULL,
    spot_count              INTEGER                 NOT NULL DEFAULT 0,
    icc_profile_used        CHARACTER VARYING(120),
    estimate_date           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    duration_ms             BIGINT                  NOT NULL,
    user_id                 CHARACTER VARYING(64),
    CONSTRAINT ink_estimate_history_pkey PRIMARY KEY (ink_estimate_history_id),
    CONSTRAINT ink_estimate_history_sizes_check
        CHECK (original_size_bytes >= 0),
    CONSTRAINT ink_estimate_history_dims_check
        CHECK (width_cm > 0 AND height_cm > 0),
    CONSTRAINT ink_estimate_history_sheets_check
        CHECK (sheet_count > 0),
    CONSTRAINT ink_estimate_history_dpi_check
        CHECK (dpi > 0),
    CONSTRAINT ink_estimate_history_duration_check
        CHECK (duration_ms >= 0)
);

CREATE INDEX idx_ink_estimate_history_user_id ON indicolors.ink_estimate_history (user_id);
CREATE INDEX idx_ink_estimate_history_estimate_date ON indicolors.ink_estimate_history (estimate_date DESC);

COMMENT ON TABLE indicolors.ink_estimate_history IS 'Metadatos de estimaciones de consumo de tinta (el binario no se persiste)';

GRANT ALL PRIVILEGES ON TABLE indicolors.ink_estimate_history TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.ink_estimate_history TO indicolors_app;

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
    CONSTRAINT production_orders_requested_quantity_check CHECK (requested_quantity > 0),
    CONSTRAINT production_orders_status_check CHECK (status IN (
        'PENDING',
        'PAUSED',
        'UNDER_REVIEW',
        'IN_PROGRESS',
        'IN_PROGRESS_PREPRESS',
        'IN_PROGRESS_CUTTING',
        'IN_PROGRESS_PRINTING',
        'IN_PROGRESS_FINISHED_PRODUCTS',
        'IN_PROGRESS_FINISHING',
        'COMPLETED',
        'ANULADA'
    ))
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
COMMENT ON COLUMN indicolors.production_orders.status IS 'Estado de planta. ANULADA es el cierre/anulación de la OP (antes CANCELLED). Aliases de entrada temporal en API: CANCELLED, CANCELED, CANCELADA, ANULADO → ANULADA.';
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

-- ============================================
-- MÓDULO ESTACIÓN — Bitácora operativa de planta
-- Ver: PROMPT_BASE_DATOS_ESTACION_COMPLETO.md
-- ============================================

-- ============================================
-- 36. CREAR TABLA BITÁCORA OPERATIVA DE ESTACIÓN (append-only, fuente de verdad)
-- ============================================
CREATE TABLE indicolors.station_operation_events (
    station_operation_event_id   CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                   CHARACTER VARYING(64)       NOT NULL,
    production_order_id          CHARACTER VARYING(64),
    client_id                    CHARACTER VARYING(64),
    user_id                      CHARACTER VARYING(64)       NOT NULL,
    actor_user_id                CHARACTER VARYING(64)       NOT NULL,
    actor_name                   CHARACTER VARYING(255),
    work_name                    CHARACTER VARYING(150),
    phase                        CHARACTER VARYING(32)       NOT NULL,
    process_key                  CHARACTER VARYING(128)      NOT NULL,
    catalog_item_kind            CHARACTER VARYING(16),  -- terminado | acabado
    catalog_item_id              CHARACTER VARYING(64),  -- FK lógica a production_order_postpress_lines.catalog_item_id (catálogo maestro); no hay FK física, ver comentario de columna
    catalog_item_label           CHARACTER VARYING(255),
    event_type                   CHARACTER VARYING(32)       NOT NULL,  -- asignacion|cambio_estado_orden|entrega_parcial|entrega_total|avance_unidades|marca_horario|inicio_fase|fin_fase|paro|reanudacion
    occurred_at                  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    units                        INTEGER,
    pause_reason                 CHARACTER VARYING(64),
    note                         TEXT,
    production_status_snapshot   CHARACTER VARYING(64),
    order_status_snapshot        CHARACTER VARYING(64),
    is_shift_event                BOOLEAN                     NOT NULL DEFAULT FALSE,

    created_at                   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT station_operation_events_pkey PRIMARY KEY (station_operation_event_id),
    CONSTRAINT station_operation_events_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT station_operation_events_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id),
    CONSTRAINT station_operation_events_client_fk
        FOREIGN KEY (client_id) REFERENCES indicolors.clients (client_id),
    CONSTRAINT station_operation_events_user_fk
        FOREIGN KEY (user_id) REFERENCES indicolors.users (user_id),
    CONSTRAINT station_operation_events_actor_user_fk
        FOREIGN KEY (actor_user_id) REFERENCES indicolors.users (user_id),
    CONSTRAINT station_operation_events_order_or_shift_check
        CHECK (production_order_id IS NOT NULL OR is_shift_event = TRUE),
    CONSTRAINT station_operation_events_event_type_check CHECK (
        event_type IN (
            'asignacion', 'cambio_estado_orden', 'entrega_parcial', 'entrega_total',
            'avance_unidades', 'marca_horario', 'inicio_fase', 'fin_fase',
            'paro', 'reanudacion'
        )
    ),
    CONSTRAINT station_operation_events_catalog_item_kind_check
        CHECK (catalog_item_kind IS NULL OR catalog_item_kind IN ('terminado', 'acabado'))
);

CREATE INDEX idx_station_operation_events_company_id ON indicolors.station_operation_events (company_id);
CREATE INDEX idx_station_operation_events_order_time ON indicolors.station_operation_events (company_id, production_order_id, occurred_at DESC);
CREATE INDEX idx_station_operation_events_user_time ON indicolors.station_operation_events (company_id, user_id, occurred_at DESC);
CREATE INDEX idx_station_operation_events_client_time ON indicolors.station_operation_events (company_id, client_id, occurred_at DESC);
CREATE INDEX idx_station_operation_events_process ON indicolors.station_operation_events (production_order_id, process_key, occurred_at DESC);
CREATE INDEX idx_station_operation_events_catalog_item ON indicolors.station_operation_events (production_order_id, catalog_item_kind, catalog_item_id, occurred_at DESC)
    WHERE catalog_item_id IS NOT NULL;
CREATE INDEX idx_station_operation_events_event_type ON indicolors.station_operation_events (company_id, event_type, occurred_at DESC);
CREATE INDEX idx_station_operation_events_shift ON indicolors.station_operation_events (company_id, user_id, occurred_at DESC)
    WHERE is_shift_event = TRUE;
CREATE INDEX idx_station_operation_events_occurred_at_brin
    ON indicolors.station_operation_events USING BRIN (occurred_at);

COMMENT ON TABLE indicolors.station_operation_events IS 'Bitácora operativa append-only del módulo Estación: cada fila es un hecho ocurrido en planta (avance, pausa, entrega, jornada). No admite UPDATE ni DELETE en producción; correcciones = nuevo evento con note explicativa';

COMMENT ON COLUMN indicolors.station_operation_events.station_operation_event_id IS
    'Identificador único del evento (UUID en texto)';
COMMENT ON COLUMN indicolors.station_operation_events.company_id IS 'Identificador de la empresa dueña del evento';
COMMENT ON COLUMN indicolors.station_operation_events.production_order_id IS 'Identificador de la Orden de Producción asociada; NULL solo si is_shift_event = TRUE (jornada sin OP)';
COMMENT ON COLUMN indicolors.station_operation_events.client_id IS 'Snapshot del cliente de la OP al momento de insertar; obligatorio si hay OP';
COMMENT ON COLUMN indicolors.station_operation_events.user_id IS 'Operario asignado al proceso sobre el que ocurre el evento';
COMMENT ON COLUMN indicolors.station_operation_events.actor_user_id IS 'Usuario que ejecutó la acción (tomado del JWT), puede diferir del operario asignado';
COMMENT ON COLUMN indicolors.station_operation_events.actor_name IS 'Snapshot del nombre del actor al momento de insertar (reportes históricos)';
COMMENT ON COLUMN indicolors.station_operation_events.work_name IS 'Snapshot del nombre del trabajo/pieza de la OP al momento de insertar';
COMMENT ON COLUMN indicolors.station_operation_events.phase IS 'Etapa del wizard sobre la que ocurre el evento (preprensa, corte-papel, impresion, terminados, acabados, jornada)';
COMMENT ON COLUMN indicolors.station_operation_events.process_key IS 'Clave exacta del proceso o ítem: nombre de fase, fase plural de catálogo, terminado:{catalogItemId}, acabado:{catalogItemId} o jornada. El catalogItemId es el id del catálogo maestro (mismo valor que production_order_postpress_lines.catalog_item_id), no el id de production_order_postpress_records';
COMMENT ON COLUMN indicolors.station_operation_events.catalog_item_kind IS 'Tipo de ítem de catálogo cuando process_key referencia uno: terminado | acabado';
COMMENT ON COLUMN indicolors.station_operation_events.catalog_item_id IS 'Referencia lógica al catálogo maestro de Terminados/Acabados (mismo valor que production_order_postpress_lines.catalog_item_id); sin FK física por no ser una tabla única de origen, validar en el Service que exista al menos una línea de la OP con este catalog_item_id';
COMMENT ON COLUMN indicolors.station_operation_events.catalog_item_label IS 'Snapshot de la etiqueta del ítem de catálogo al momento de insertar';
COMMENT ON COLUMN indicolors.station_operation_events.event_type IS 'Tipo de hecho operativo registrado';
COMMENT ON COLUMN indicolors.station_operation_events.occurred_at IS 'Fecha/hora del hecho operativo (puede venir del cliente; el backend valida que no sea futuro lejano)';
COMMENT ON COLUMN indicolors.station_operation_events.units IS 'Unidades involucradas; solo aplica en avance_unidades, entrega_parcial y entrega_total';
COMMENT ON COLUMN indicolors.station_operation_events.pause_reason IS 'Motivo de la pausa; solo aplica en paro y marca_horario';
COMMENT ON COLUMN indicolors.station_operation_events.note IS 'Nota libre; usada también para explicar correcciones sobre eventos previos';
COMMENT ON COLUMN indicolors.station_operation_events.production_status_snapshot IS 'Snapshot del estado de producción en planta al momento del evento';
COMMENT ON COLUMN indicolors.station_operation_events.order_status_snapshot IS 'Snapshot de production_orders.status al momento del evento';
COMMENT ON COLUMN indicolors.station_operation_events.is_shift_event IS 'True=evento de jornada laboral sin OP asociada (orderId = __jornada__ en el SPA)';
COMMENT ON COLUMN indicolors.station_operation_events.created_at IS 'Fecha y hora de persistencia del registro (distinta de occurred_at)';

GRANT ALL PRIVILEGES ON TABLE indicolors.station_operation_events TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.station_operation_events TO indicolors_app;

-- ============================================
-- 37. CREAR TABLA INTERVALOS DE OPERACIÓN DE ESTACIÓN (derivada, para reportes de tiempo)
-- ============================================
CREATE TABLE indicolors.station_operation_intervals (
    station_operation_interval_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                    CHARACTER VARYING(64)       NOT NULL,
    production_order_id           CHARACTER VARYING(64),
    client_id                     CHARACTER VARYING(64),
    user_id                       CHARACTER VARYING(64)       NOT NULL,
    process_key                   CHARACTER VARYING(128),
    phase                         CHARACTER VARYING(32),
    catalog_item_id               CHARACTER VARYING(64),  -- FK lógica a production_order_postpress_lines.catalog_item_id (catálogo maestro); no hay FK física
    interval_kind                 CHARACTER VARYING(16)       NOT NULL,  -- labor | pause | shift
    started_at                    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    ended_at                      TIMESTAMP WITHOUT TIME ZONE,
    duration_ms                   BIGINT,
    pause_reason                  CHARACTER VARYING(64),
    note                          TEXT,
    opened_by_event_id            CHARACTER VARYING(64)       NOT NULL,
    closed_by_event_id            CHARACTER VARYING(64),
    is_open                       BOOLEAN                     NOT NULL DEFAULT TRUE,

    created_at                    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT station_operation_intervals_pkey PRIMARY KEY (station_operation_interval_id),
    CONSTRAINT station_operation_intervals_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT station_operation_intervals_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id),
    CONSTRAINT station_operation_intervals_client_fk
        FOREIGN KEY (client_id) REFERENCES indicolors.clients (client_id),
    CONSTRAINT station_operation_intervals_user_fk
        FOREIGN KEY (user_id) REFERENCES indicolors.users (user_id),
    CONSTRAINT station_operation_intervals_opened_by_event_fk
        FOREIGN KEY (opened_by_event_id) REFERENCES indicolors.station_operation_events (station_operation_event_id),
    CONSTRAINT station_operation_intervals_closed_by_event_fk
        FOREIGN KEY (closed_by_event_id) REFERENCES indicolors.station_operation_events (station_operation_event_id),
    CONSTRAINT station_operation_intervals_kind_check
        CHECK (interval_kind IN ('labor', 'pause', 'shift')),
    CONSTRAINT station_operation_intervals_duration_check
        CHECK (duration_ms IS NULL OR duration_ms >= 0),
    CONSTRAINT station_operation_intervals_open_consistency_check
        CHECK (is_open = FALSE OR (ended_at IS NULL AND closed_by_event_id IS NULL))
);

CREATE INDEX idx_station_operation_intervals_user_range ON indicolors.station_operation_intervals (company_id, user_id, started_at, ended_at);
CREATE INDEX idx_station_operation_intervals_order_process ON indicolors.station_operation_intervals (production_order_id, process_key, started_at)
    WHERE production_order_id IS NOT NULL;
CREATE INDEX idx_station_operation_intervals_catalog_item ON indicolors.station_operation_intervals (production_order_id, catalog_item_id, started_at)
    WHERE catalog_item_id IS NOT NULL;
CREATE INDEX idx_station_operation_intervals_open ON indicolors.station_operation_intervals (company_id, user_id, is_open)
    WHERE is_open = TRUE;
CREATE INDEX idx_station_operation_intervals_started_at_brin
    ON indicolors.station_operation_intervals USING BRIN (started_at);

COMMENT ON TABLE indicolors.station_operation_intervals IS 'Intervalos de labor/pausa/jornada materializados a partir de station_operation_events, para reportes de tiempo sin recalcular en cada request. Se puebla en la misma transacción del evento (servicio de aplicación) o vía trigger';

COMMENT ON COLUMN indicolors.station_operation_intervals.station_operation_interval_id IS
    'Identificador único del intervalo (UUID en texto)';
COMMENT ON COLUMN indicolors.station_operation_intervals.company_id IS 'Identificador de la empresa dueña del intervalo';
COMMENT ON COLUMN indicolors.station_operation_intervals.production_order_id IS 'Identificador de la Orden de Producción asociada; NULL en intervalos de jornada (shift)';
COMMENT ON COLUMN indicolors.station_operation_intervals.client_id IS 'Snapshot del cliente de la OP asociada';
COMMENT ON COLUMN indicolors.station_operation_intervals.user_id IS 'Operario dueño del intervalo';
COMMENT ON COLUMN indicolors.station_operation_intervals.process_key IS 'Clave del proceso o ítem al que pertenece el intervalo (ver process_key en station_operation_events)';
COMMENT ON COLUMN indicolors.station_operation_intervals.phase IS 'Etapa del wizard a la que pertenece el intervalo';
COMMENT ON COLUMN indicolors.station_operation_intervals.catalog_item_id IS 'Referencia lógica al catálogo maestro de Terminados/Acabados (mismo valor que production_order_postpress_lines.catalog_item_id); sin FK física, validar en el Service';
COMMENT ON COLUMN indicolors.station_operation_intervals.interval_kind IS 'Tipo de intervalo: labor (trabajo activo) | pause (paro) | shift (jornada)';
COMMENT ON COLUMN indicolors.station_operation_intervals.started_at IS 'Inicio del intervalo';
COMMENT ON COLUMN indicolors.station_operation_intervals.ended_at IS 'Fin del intervalo; NULL mientras is_open = TRUE';
COMMENT ON COLUMN indicolors.station_operation_intervals.duration_ms IS 'Duración en milisegundos, calculada al cerrar el intervalo';
COMMENT ON COLUMN indicolors.station_operation_intervals.pause_reason IS 'Motivo de la pausa; solo aplica cuando interval_kind = pause';
COMMENT ON COLUMN indicolors.station_operation_intervals.note IS 'Nota libre asociada al intervalo';
COMMENT ON COLUMN indicolors.station_operation_intervals.opened_by_event_id IS
    'Evento de station_operation_events que abrió el intervalo';
COMMENT ON COLUMN indicolors.station_operation_intervals.closed_by_event_id IS
    'Evento de station_operation_events que cerró el intervalo';
COMMENT ON COLUMN indicolors.station_operation_intervals.is_open IS 'True=el intervalo sigue abierto (sin ended_at ni closed_by_event_id)';
COMMENT ON COLUMN indicolors.station_operation_intervals.created_at IS 'Fecha y hora de creación del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.station_operation_intervals TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.station_operation_intervals TO indicolors_app;

-- ============================================
-- 38. CREAR TABLA PROGRESO AGREGADO DE PROCESO DE ESTACIÓN (opcional, performance de inbox)
-- ============================================
CREATE TABLE indicolors.station_process_progress (
    company_id            CHARACTER VARYING(64)       NOT NULL,
    production_order_id   CHARACTER VARYING(64)       NOT NULL,
    process_key           CHARACTER VARYING(128)      NOT NULL,
    user_id               CHARACTER VARYING(64)       NOT NULL,
    phase                 CHARACTER VARYING(32)       NOT NULL,
    catalog_item_id       CHARACTER VARYING(64),  -- FK lógica a production_order_postpress_lines.catalog_item_id (catálogo maestro); no hay FK física
    total_units           INTEGER                     NOT NULL DEFAULT 0,
    completed_units       INTEGER                     NOT NULL DEFAULT 0,
    delivered_units       INTEGER                     NOT NULL DEFAULT 0,
    status                CHARACTER VARYING(16)       NOT NULL DEFAULT 'pendiente',
    last_event_at         TIMESTAMP WITHOUT TIME ZONE,

    created_at             TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT station_process_progress_pkey PRIMARY KEY (production_order_id, process_key),
    CONSTRAINT station_process_progress_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT station_process_progress_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT station_process_progress_user_fk
        FOREIGN KEY (user_id) REFERENCES indicolors.users (user_id),
    CONSTRAINT station_process_progress_status_check
        CHECK (status IN ('pendiente', 'en-proceso', 'terminado')),
    CONSTRAINT station_process_progress_units_check
        CHECK (total_units >= 0 AND completed_units >= 0 AND delivered_units >= 0)
);

CREATE INDEX idx_station_process_progress_user ON indicolors.station_process_progress (company_id, user_id, status);
CREATE INDEX idx_station_process_progress_company_id ON indicolors.station_process_progress (company_id);

COMMENT ON TABLE indicolors.station_process_progress IS 'Agregados de progreso por proceso/ítem de una OP, para inbox y listados rápidos; se recalcula desde station_operation_events y se actualiza en la misma transacción que inserta el evento. Opcional en v1: la UI puede calcular desde eventos si esta tabla no existe';

COMMENT ON COLUMN indicolors.station_process_progress.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.station_process_progress.production_order_id IS 'Identificador de la Orden de Producción a la que pertenece el proceso';
COMMENT ON COLUMN indicolors.station_process_progress.process_key IS 'Clave del proceso o ítem agregado (ver process_key en station_operation_events)';
COMMENT ON COLUMN indicolors.station_process_progress.user_id IS 'Operario asignado al proceso';
COMMENT ON COLUMN indicolors.station_process_progress.phase IS 'Etapa del wizard a la que pertenece el proceso';
COMMENT ON COLUMN indicolors.station_process_progress.catalog_item_id IS 'Referencia lógica al catálogo maestro de Terminados/Acabados (mismo valor que production_order_postpress_lines.catalog_item_id); sin FK física, validar en el Service';
COMMENT ON COLUMN indicolors.station_process_progress.total_units IS 'Unidades totales esperadas para el proceso/ítem';
COMMENT ON COLUMN indicolors.station_process_progress.completed_units IS 'Unidades procesadas acumuladas (avance_unidades)';
COMMENT ON COLUMN indicolors.station_process_progress.delivered_units IS 'Unidades entregadas acumuladas (entrega_parcial/entrega_total)';
COMMENT ON COLUMN indicolors.station_process_progress.status IS 'Estado agregado del proceso/ítem: pendiente | en-proceso | terminado';
COMMENT ON COLUMN indicolors.station_process_progress.last_event_at IS 'occurred_at del último evento que actualizó este agregado';
COMMENT ON COLUMN indicolors.station_process_progress.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.station_process_progress.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.station_process_progress TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.station_process_progress TO indicolors_app;

-- Agregado de cantidad disponible por OP (módulo Estación). Una fila por orden.
CREATE TABLE indicolors.station_order_progress (
    company_id            CHARACTER VARYING(64)       NOT NULL,
    production_order_id   CHARACTER VARYING(64)       NOT NULL,
    cantidad_disponible   INTEGER                     NOT NULL DEFAULT 0,
    updated_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT station_order_progress_pkey PRIMARY KEY (production_order_id),
    CONSTRAINT station_order_progress_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT station_order_progress_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT station_order_progress_cantidad_check
        CHECK (cantidad_disponible >= 0)
);

CREATE INDEX idx_station_order_progress_company
    ON indicolors.station_order_progress (company_id, cantidad_disponible);

COMMENT ON TABLE indicolors.station_order_progress IS
    'Agregado de cantidad disponible para entrega comercial por OP. cantidad_disponible = MIN(unidades procesadas por proceso real de la OP); no resta pedidos OPE. Se recalcula en la misma transacción que inserta avance_unidades.';

COMMENT ON COLUMN indicolors.station_order_progress.company_id IS 'Empresa dueña del registro (multi-tenant)';
COMMENT ON COLUMN indicolors.station_order_progress.production_order_id IS 'OP (PK; una fila por orden)';
COMMENT ON COLUMN indicolors.station_order_progress.cantidad_disponible IS 'Unidades disponibles para pedidos comerciales (≥ 0)';
COMMENT ON COLUMN indicolors.station_order_progress.updated_at IS 'Última actualización del agregado';

GRANT ALL PRIVILEGES ON TABLE indicolors.station_order_progress TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.station_order_progress TO indicolors_app;

-- ============================================
-- MÓDULO PEDIDOS / CUENTAS POR COBRAR / ABONOS
-- Continuación de Script_Crear_BD.sql (después de la sección 38,
-- station_order_progress). Sigue exactamente las mismas convenciones:
-- ids CHARACTER VARYING(64) con gen_random_uuid()::text, company_id en
-- todas las tablas, TIMESTAMP WITHOUT TIME ZONE, comentarios y GRANTs
-- explícitos por tabla.
--
-- Principio de diseño (igual que Estación): las tablas nuevas NO
-- modifican production_orders ni las tablas de Estación. order_deliveries
-- y order_payments son ledgers append-only (fuente de verdad);
-- accounts_receivable es una tabla DERIVADA que se mantiene
-- sincronizada con triggers, en la misma transacción del INSERT, para
-- que nunca pueda desincronizarse por un olvido en el backend.
-- customer_orders es la cabecera 1:1 del pedido comercial (ODP-{n}), creada
-- al pasar la OP a IN_PROGRESS* (distinta de production_orders).
-- ============================================

-- ============================================
-- 38.0 CREAR TABLA SECUENCIA DE NÚMEROS ODP DE PEDIDO (customer_orders.odp_number)
-- ============================================
CREATE TABLE indicolors.customer_orders_number_sequences (
    company_id  CHARACTER VARYING(64) NOT NULL,
    last_value  BIGINT                NOT NULL DEFAULT 0,
    CONSTRAINT customer_orders_number_sequences_pkey PRIMARY KEY (company_id),
    CONSTRAINT customer_orders_number_sequences_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT customer_orders_number_sequences_last_value_check CHECK (last_value >= 0)
);

COMMENT ON TABLE indicolors.customer_orders_number_sequences IS
    'Último consecutivo de odp_number emitido por compañía; se incrementa al crear un pedido comercial (customer_orders)';
COMMENT ON COLUMN indicolors.customer_orders_number_sequences.company_id IS
    'Identificador de la empresa dueña del contador';
COMMENT ON COLUMN indicolors.customer_orders_number_sequences.last_value IS
    'Último número asignado (el odp_number expuesto es ODP-{last_value})';

GRANT ALL PRIVILEGES ON TABLE indicolors.customer_orders_number_sequences TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.customer_orders_number_sequences TO indicolors_app;

-- ============================================
-- 38.0b CREAR TABLA PEDIDOS COMERCIALES (customer_orders) — 1:1 con production_orders
-- ============================================
CREATE TABLE indicolors.customer_orders (
    customer_order_id              CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id            CHARACTER VARYING(64)       NOT NULL,
    odp_number            CHARACTER VARYING(32)       NOT NULL,
    production_order_id   CHARACTER VARYING(64)       NOT NULL,
    client_id             CHARACTER VARYING(64)       NOT NULL,
    created_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    created_by            CHARACTER VARYING(64)       NOT NULL,

    CONSTRAINT customer_orders_pkey PRIMARY KEY (customer_order_id),
    CONSTRAINT customer_orders_odp_number_company_unique UNIQUE (company_id, odp_number),
    CONSTRAINT customer_orders_production_order_unique UNIQUE (production_order_id),
    CONSTRAINT customer_orders_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT customer_orders_production_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id),
    CONSTRAINT customer_orders_client_fk
        FOREIGN KEY (client_id) REFERENCES indicolors.clients (client_id),
    CONSTRAINT customer_orders_created_by_fk
        FOREIGN KEY (created_by) REFERENCES indicolors.users (user_id),
    CONSTRAINT customer_orders_odp_number_format_check
        CHECK (odp_number ~ '^ODP-[0-9]+$')
);

CREATE INDEX idx_customer_orders_company_id
    ON indicolors.customer_orders (company_id);
CREATE INDEX idx_customer_orders_client_id
    ON indicolors.customer_orders (company_id, client_id);

COMMENT ON TABLE indicolors.customer_orders IS
    'Cabecera de pedido comercial (1:1 con production_orders). Distinto de la OP de planta. Se crea cuando la OP entra a IN_PROGRESS*; no es el ledger de entregas';
COMMENT ON COLUMN indicolors.customer_orders.customer_order_id IS
    'Identificador único (UUID) del pedido comercial';
COMMENT ON COLUMN indicolors.customer_orders.company_id IS
    'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.customer_orders.odp_number IS
    'Consecutivo corto del pedido por compañía (ej. ODP-1, ODP-42), generado por el backend; único por compañía';
COMMENT ON COLUMN indicolors.customer_orders.production_order_id IS
    'OP de planta asociada (1:1, forzado por UNIQUE)';
COMMENT ON COLUMN indicolors.customer_orders.client_id IS
    'Cliente snapshot desde production_orders.client_id al crear el pedido';
COMMENT ON COLUMN indicolors.customer_orders.created_at IS
    'Fecha y hora de creación del pedido';
COMMENT ON COLUMN indicolors.customer_orders.created_by IS
    'Usuario que disparó la transición a progreso (y por tanto la creación del pedido)';

GRANT ALL PRIVILEGES ON TABLE indicolors.customer_orders TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.customer_orders TO indicolors_app;

-- ============================================
-- 38.1 CREAR TABLA SECUENCIA DE NÚMEROS ODP DE ENTREGA (consecutivo atómico por empresa)
-- ============================================
CREATE TABLE indicolors.order_delivery_number_sequences (
    company_id  CHARACTER VARYING(64) NOT NULL,
    last_value  BIGINT                NOT NULL DEFAULT 0,
    CONSTRAINT order_delivery_number_sequences_pkey PRIMARY KEY (company_id),
    CONSTRAINT order_delivery_number_sequences_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT order_delivery_number_sequences_last_value_check CHECK (last_value >= 0)
);

COMMENT ON TABLE indicolors.order_delivery_number_sequences IS
    'Último consecutivo de delivery_number emitido por compañía; se incrementa de forma atómica al crear una entrega';
COMMENT ON COLUMN indicolors.order_delivery_number_sequences.company_id IS
    'Identificador de la empresa dueña del contador';
COMMENT ON COLUMN indicolors.order_delivery_number_sequences.last_value IS
    'Último número asignado (el delivery_number expuesto es ODP-{last_value})';

GRANT ALL PRIVILEGES ON TABLE indicolors.order_delivery_number_sequences TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.order_delivery_number_sequences TO indicolors_app;

-- ============================================
-- 39. CREAR TABLA ENTREGAS DE PEDIDO (append-only, fuente de verdad comercial)
-- ============================================
CREATE TABLE indicolors.order_deliveries (
    order_delivery_id     CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id            CHARACTER VARYING(64)       NOT NULL,
    delivery_number       CHARACTER VARYING(32)       NOT NULL,
    production_order_id   CHARACTER VARYING(64)       NOT NULL,
    client_id             CHARACTER VARYING(64)       NOT NULL,
    seller_id             CHARACTER VARYING(64),

    movement_type         CHARACTER VARYING(16)       NOT NULL DEFAULT 'entrega',  -- entrega | reversion
    delivery_type         CHARACTER VARYING(16)       NOT NULL,  -- parcial | total (subtipo, aplica a ambos movement_type)
    reversed_delivery_id  CHARACTER VARYING(64),  -- obligatorio si movement_type='reversion'
    quantity_delivered    INTEGER                     NOT NULL,
    unit_price            NUMERIC(12,2)               NOT NULL DEFAULT 0,
    total_value           NUMERIC(14,2)               NOT NULL DEFAULT 0,
    available_before      INTEGER                     NOT NULL DEFAULT 0,  -- calculado por trigger, no lo envía el cliente

    work_name_snapshot    CHARACTER VARYING(150),
    client_name_snapshot  CHARACTER VARYING(200),

    delivered_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    delivered_by          CHARACTER VARYING(64)       NOT NULL,
    notes                 TEXT,

    created_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT order_deliveries_pkey PRIMARY KEY (order_delivery_id),
    CONSTRAINT order_deliveries_delivery_number_company_unique UNIQUE (company_id, delivery_number),
    CONSTRAINT order_deliveries_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT order_deliveries_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id),
    CONSTRAINT order_deliveries_client_fk
        FOREIGN KEY (client_id) REFERENCES indicolors.clients (client_id),
    CONSTRAINT order_deliveries_seller_fk
        FOREIGN KEY (seller_id) REFERENCES indicolors.sellers (seller_id),
    CONSTRAINT order_deliveries_delivered_by_fk
        FOREIGN KEY (delivered_by) REFERENCES indicolors.users (user_id),
    CONSTRAINT order_deliveries_reversed_fk
        FOREIGN KEY (reversed_delivery_id) REFERENCES indicolors.order_deliveries (order_delivery_id),
    CONSTRAINT order_deliveries_type_check
        CHECK (delivery_type IN ('parcial', 'total')),
    CONSTRAINT order_deliveries_movement_type_check
        CHECK (movement_type IN ('entrega', 'reversion')),
    CONSTRAINT order_deliveries_reversion_ref_check
        CHECK (movement_type <> 'reversion' OR reversed_delivery_id IS NOT NULL),
    CONSTRAINT order_deliveries_entrega_no_ref_check
        CHECK (movement_type <> 'entrega' OR reversed_delivery_id IS NULL),
    CONSTRAINT order_deliveries_quantity_check
        CHECK (quantity_delivered > 0),
    CONSTRAINT order_deliveries_unit_price_check
        CHECK (unit_price >= 0),
    CONSTRAINT order_deliveries_available_before_check
        CHECK (available_before >= 0),
    CONSTRAINT order_deliveries_delivery_number_format_check
        CHECK (delivery_number ~ '^ODP-[0-9]+$')
);

CREATE INDEX idx_order_deliveries_company_id ON indicolors.order_deliveries (company_id);
CREATE INDEX idx_order_deliveries_order_time ON indicolors.order_deliveries (company_id, production_order_id, delivered_at DESC);
CREATE INDEX idx_order_deliveries_client_time ON indicolors.order_deliveries (company_id, client_id, delivered_at DESC);
CREATE INDEX idx_order_deliveries_seller_time ON indicolors.order_deliveries (company_id, seller_id, delivered_at DESC)
    WHERE seller_id IS NOT NULL;
CREATE INDEX idx_order_deliveries_delivery_number ON indicolors.order_deliveries (company_id, delivery_number);

-- Una sola reversión activa por entrega original (append-only: no se
-- puede anular dos veces la misma entrega).
CREATE UNIQUE INDEX uq_order_deliveries_reversed_once
    ON indicolors.order_deliveries (company_id, reversed_delivery_id)
    WHERE reversed_delivery_id IS NOT NULL;

COMMENT ON TABLE indicolors.order_deliveries IS 'Bitácora append-only de entregas comerciales (parciales/totales) y sus reversiones (movement_type=reversion) de una OP a su cliente/representante. No admite UPDATE ni DELETE en producción; correcciones = nueva fila (reversión) con notes explicativa';

COMMENT ON COLUMN indicolors.order_deliveries.order_delivery_id IS 'Identificador único de la entrega o de su reversión';
COMMENT ON COLUMN indicolors.order_deliveries.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.order_deliveries.delivery_number IS 'Consecutivo corto del pedido/entrega por compañía (ej. ODP-1, ODP-42), generado por el backend; único por compañía; cada reversión también recibe el suyo propio';
COMMENT ON COLUMN indicolors.order_deliveries.production_order_id IS 'Identificador de la Orden de Producción entregada';
COMMENT ON COLUMN indicolors.order_deliveries.client_id IS 'Cliente que recibe la entrega (snapshot desde production_orders.client_id)';
COMMENT ON COLUMN indicolors.order_deliveries.seller_id IS 'Representante/vendedor asociado a la entrega, si aplica';
COMMENT ON COLUMN indicolors.order_deliveries.movement_type IS 'entrega=movimiento normal que suma disponibilidad entregada | reversion=anulación de una entrega previa que la resta (append-only, nunca UPDATE/DELETE)';
COMMENT ON COLUMN indicolors.order_deliveries.delivery_type IS 'parcial=entrega parcial de unidades | total=entrega final que cierra la OP comercialmente; en una reversión describe el subtipo de la entrega original que se anula';
COMMENT ON COLUMN indicolors.order_deliveries.reversed_delivery_id IS 'order_delivery_id de la entrega original que se anula; obligatorio si movement_type=reversion, y solo puede apuntar a una fila con movement_type=entrega que aún no haya sido revertida';
COMMENT ON COLUMN indicolors.order_deliveries.quantity_delivered IS 'Unidades del movimiento (> 0); en una reversión es la misma cantidad de la entrega original que se está anulando, el trigger la resta';
COMMENT ON COLUMN indicolors.order_deliveries.unit_price IS 'Precio unitario snapshot usado para valorizar esta entrega';
COMMENT ON COLUMN indicolors.order_deliveries.total_value IS 'quantity_delivered * unit_price, calculado por el backend al insertar; en una reversión debe igualar el total_value de la entrega original';
COMMENT ON COLUMN indicolors.order_deliveries.available_before IS 'Unidades disponibles para entrega justo antes de este movimiento; lo calcula el trigger de validación, no lo envía el cliente';
COMMENT ON COLUMN indicolors.order_deliveries.work_name_snapshot IS 'Snapshot de production_orders.work_name al momento de insertar';
COMMENT ON COLUMN indicolors.order_deliveries.client_name_snapshot IS 'Snapshot de clients.name al momento de insertar';
COMMENT ON COLUMN indicolors.order_deliveries.delivered_at IS 'Fecha/hora real de la entrega (puede venir del cliente)';
COMMENT ON COLUMN indicolors.order_deliveries.delivered_by IS 'Usuario que registró la entrega o la reversión';
COMMENT ON COLUMN indicolors.order_deliveries.notes IS 'Nota libre; usada también para explicar anulaciones';
COMMENT ON COLUMN indicolors.order_deliveries.created_at IS 'Fecha y hora de persistencia del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.order_deliveries TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.order_deliveries TO indicolors_app;

-- ============================================
-- 39.1 CREAR TABLA SECUENCIA DE NÚMEROS ABN (consecutivo atómico por empresa)
-- ============================================
CREATE TABLE indicolors.order_payment_number_sequences (
    company_id  CHARACTER VARYING(64) NOT NULL,
    last_value  BIGINT                NOT NULL DEFAULT 0,
    CONSTRAINT order_payment_number_sequences_pkey PRIMARY KEY (company_id),
    CONSTRAINT order_payment_number_sequences_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT order_payment_number_sequences_last_value_check CHECK (last_value >= 0)
);

COMMENT ON TABLE indicolors.order_payment_number_sequences IS
    'Consecutivo ABN-{n} por compañía: payment_number (movimientos) y abonos_number (id del agregado de Abonos). '
    'Compartido para que el id del agregado nunca coincida con un payment_number.';
COMMENT ON COLUMN indicolors.order_payment_number_sequences.company_id IS
    'Identificador de la empresa dueña del contador';
COMMENT ON COLUMN indicolors.order_payment_number_sequences.last_value IS
    'Último ABN asignado (payment_number o abonos_number = ABN-{last_value})';

GRANT ALL PRIVILEGES ON TABLE indicolors.order_payment_number_sequences TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.order_payment_number_sequences TO indicolors_app;

-- ============================================
-- 40. CREAR TABLA ABONOS DE PEDIDO (append-only, ledger de liquidaciones)
-- ============================================
-- Abonos/pagos append-only (módulo Pedidos / Abonos).
-- Tipos de liquidación: abono | anticipo | retencion | reversion (Opción A, listo para FE).

CREATE TABLE indicolors.order_payments (
    order_payment_id      CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id            CHARACTER VARYING(64)       NOT NULL,
    payment_number        CHARACTER VARYING(32)       NOT NULL,
    production_order_id   CHARACTER VARYING(64)       NOT NULL,
    client_id             CHARACTER VARYING(64)       NOT NULL,

    payment_type          CHARACTER VARYING(16)       NOT NULL DEFAULT 'abono',
    amount                NUMERIC(14,2)               NOT NULL,
    payment_method        CHARACTER VARYING(32)       NOT NULL,
    reference             CHARACTER VARYING(100),
    reversed_payment_id   CHARACTER VARYING(64),

    withholding_type      CHARACTER VARYING(32),
    withholding_base      NUMERIC(14,2),
    withholding_rate      NUMERIC(8,4),
    certificate_ref       CHARACTER VARYING(100),

    -- Reserva para facturación electrónica (imputación futura; sin FK aún).
    invoice_id            CHARACTER VARYING(64),

    paid_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    registered_by         CHARACTER VARYING(64)       NOT NULL,
    notes                 TEXT,

    created_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT order_payments_pkey PRIMARY KEY (order_payment_id),
    CONSTRAINT order_payments_payment_number_company_unique UNIQUE (company_id, payment_number),
    CONSTRAINT order_payments_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT order_payments_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id),
    CONSTRAINT order_payments_client_fk
        FOREIGN KEY (client_id) REFERENCES indicolors.clients (client_id),
    CONSTRAINT order_payments_registered_by_fk
        FOREIGN KEY (registered_by) REFERENCES indicolors.users (user_id),
    CONSTRAINT order_payments_reversed_fk
        FOREIGN KEY (reversed_payment_id) REFERENCES indicolors.order_payments (order_payment_id),
    CONSTRAINT order_payments_type_check
        CHECK (payment_type IN ('abono', 'anticipo', 'retencion', 'reversion')),
    CONSTRAINT order_payments_amount_check
        CHECK (amount > 0),
    CONSTRAINT order_payments_method_check
        CHECK (payment_method IN ('efectivo', 'transferencia', 'cheque', 'tarjeta', 'otro', 'retencion')),
    CONSTRAINT order_payments_reversion_ref_check
        CHECK (payment_type <> 'reversion' OR reversed_payment_id IS NOT NULL),
    CONSTRAINT order_payments_non_reversion_no_ref_check
        CHECK (payment_type = 'reversion' OR reversed_payment_id IS NULL),
    CONSTRAINT order_payments_retencion_method_check
        CHECK (payment_type <> 'retencion' OR payment_method = 'retencion'),
    CONSTRAINT order_payments_cash_method_check
        CHECK (payment_type NOT IN ('abono', 'anticipo')
              OR payment_method IN ('efectivo', 'transferencia', 'cheque', 'tarjeta', 'otro')),
    CONSTRAINT order_payments_retencion_fields_check
        CHECK (payment_type <> 'retencion' OR withholding_type IS NOT NULL),
    CONSTRAINT order_payments_withholding_type_check
        CHECK (withholding_type IS NULL
               OR withholding_type IN ('retefuente', 'reteiva', 'reteica', 'otro')),
    CONSTRAINT order_payments_withholding_base_check
        CHECK (withholding_base IS NULL OR withholding_base >= 0),
    CONSTRAINT order_payments_withholding_rate_check
        CHECK (withholding_rate IS NULL OR withholding_rate >= 0),
    CONSTRAINT order_payments_payment_number_format_check
        CHECK (payment_number ~ '^ABN-[0-9]+$')
);

CREATE INDEX idx_order_payments_company_id ON indicolors.order_payments (company_id);
CREATE INDEX idx_order_payments_order_time ON indicolors.order_payments (company_id, production_order_id, paid_at DESC);
CREATE INDEX idx_order_payments_client_time ON indicolors.order_payments (company_id, client_id, paid_at DESC);
CREATE INDEX idx_order_payments_payment_number ON indicolors.order_payments (company_id, payment_number);
CREATE INDEX idx_order_payments_invoice_id ON indicolors.order_payments (company_id, invoice_id)
    WHERE invoice_id IS NOT NULL;
CREATE UNIQUE INDEX uq_order_payments_reversed_once
    ON indicolors.order_payments (company_id, reversed_payment_id)
    WHERE reversed_payment_id IS NOT NULL;

COMMENT ON TABLE indicolors.order_payments IS
    'Bitácora append-only de liquidaciones (abono/anticipo/retencion) y reversiones contra una OP. Nunca UPDATE/DELETE; anular = nueva fila reversion';

COMMENT ON COLUMN indicolors.order_payments.order_payment_id IS 'Identificador único del movimiento de pago';
COMMENT ON COLUMN indicolors.order_payments.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.order_payments.payment_number IS 'Consecutivo corto por compañía (ABN-{n}), generado por el backend';
COMMENT ON COLUMN indicolors.order_payments.production_order_id IS 'Orden de Producción contra la que se liquida';
COMMENT ON COLUMN indicolors.order_payments.client_id IS 'Cliente snapshot desde production_orders.client_id';
COMMENT ON COLUMN indicolors.order_payments.payment_type IS
    'abono=caja | anticipo=saldo a favor/pasivo operativo | retencion=liquidación fiscal sin caja | reversion=anulación';
COMMENT ON COLUMN indicolors.order_payments.amount IS 'Monto del movimiento, siempre positivo; el signo lo aplica el trigger según payment_type';
COMMENT ON COLUMN indicolors.order_payments.payment_method IS
    'Canal de caja, o retencion cuando payment_type=retencion';
COMMENT ON COLUMN indicolors.order_payments.reference IS 'Número de transferencia, cheque o comprobante, si aplica';
COMMENT ON COLUMN indicolors.order_payments.reversed_payment_id IS
    'order_payment_id del movimiento que se anula; obligatorio si payment_type=reversion';
COMMENT ON COLUMN indicolors.order_payments.withholding_type IS
    'Tipo de retención sufrida (retefuente|reteiva|reteica|otro); obligatorio si payment_type=retencion';
COMMENT ON COLUMN indicolors.order_payments.withholding_base IS 'Base gravable usada para calcular la retención';
COMMENT ON COLUMN indicolors.order_payments.withholding_rate IS 'Porcentaje aplicado (ej. 2.5000 = 2.5%)';
COMMENT ON COLUMN indicolors.order_payments.certificate_ref IS 'Número/referencia del certificado de retención';
COMMENT ON COLUMN indicolors.order_payments.invoice_id IS
    'Reserva para imputación a factura electrónica (nullable hasta FE)';
COMMENT ON COLUMN indicolors.order_payments.paid_at IS 'Fecha/hora real del cobro/liquidación';
COMMENT ON COLUMN indicolors.order_payments.registered_by IS 'Usuario que registró el movimiento';
COMMENT ON COLUMN indicolors.order_payments.notes IS 'Nota libre asociada al movimiento';
COMMENT ON COLUMN indicolors.order_payments.created_at IS 'Fecha y hora de persistencia del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.order_payments TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.order_payments TO indicolors_app;

-- ============================================
-- 40.1 CREAR TABLA SECUENCIA DE NÚMEROS CXC (consecutivo atómico por empresa)
-- ============================================
CREATE TABLE indicolors.accounts_receivable_number_sequences (
    company_id  CHARACTER VARYING(64) NOT NULL,
    last_value  BIGINT                NOT NULL DEFAULT 0,
    CONSTRAINT accounts_receivable_number_sequences_pkey PRIMARY KEY (company_id),
    CONSTRAINT accounts_receivable_number_sequences_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT accounts_receivable_number_sequences_last_value_check CHECK (last_value >= 0)
);

COMMENT ON TABLE indicolors.accounts_receivable_number_sequences IS
    'Último consecutivo de cxc_number emitido por compañía; se incrementa de forma atómica al crear una Cuenta por cobrar';
COMMENT ON COLUMN indicolors.accounts_receivable_number_sequences.company_id IS
    'Identificador de la empresa dueña del contador';
COMMENT ON COLUMN indicolors.accounts_receivable_number_sequences.last_value IS
    'Último número asignado (el cxc_number expuesto es CXC-{last_value})';

GRANT ALL PRIVILEGES ON TABLE indicolors.accounts_receivable_number_sequences TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.accounts_receivable_number_sequences TO indicolors_app;

-- ============================================
-- 40.2 FUNCIÓN — Siguiente cxc_number atómico por empresa
-- ============================================
-- accounts_receivable la inserta un trigger (no el backend), por eso el número
-- visible no puede asignarse antes del INSERT como en OP/ODP/ABN: esta
-- función hace el UPSERT atómico sobre accounts_receivable_number_sequences y
-- devuelve 'CXC-{n}'. Solo debe invocarse en la rama de INSERT de los
-- triggers de sincronización (nunca en la rama de UPDATE).
CREATE OR REPLACE FUNCTION indicolors.fn_next_cxc_number(p_company_id CHARACTER VARYING)
RETURNS CHARACTER VARYING AS $$
DECLARE
    v_next BIGINT;
BEGIN
    INSERT INTO indicolors.accounts_receivable_number_sequences (company_id, last_value)
    VALUES (p_company_id, 1)
    ON CONFLICT (company_id) DO UPDATE
        SET last_value = indicolors.accounts_receivable_number_sequences.last_value + 1
    RETURNING last_value INTO v_next;

    RETURN 'CXC-' || v_next;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION indicolors.fn_next_cxc_number(CHARACTER VARYING) IS 'Incrementa de forma atómica accounts_receivable_number_sequences para la compañía dada y devuelve el siguiente cxc_number (CXC-{n})';

-- ============================================
-- fn_next_abonos_number (ABN-{n}; secuencia compartida con payment_number)
-- ============================================
-- abonos_number (id del agregado de Abonos) = ABN-{n}, mismo prefijo que payment_number.
-- Comparte order_payment_number_sequences para garantizar que, por compañía,
-- abonos_number nunca coincida con un payment_number (movimientos). Sin tabla aparte.
-- Lo consumen los triggers de accounts_receivable vía fn_next_abonos_number (no el backend).

CREATE OR REPLACE FUNCTION indicolors.fn_next_abonos_number(p_company_id CHARACTER VARYING)
RETURNS CHARACTER VARYING AS $$
DECLARE
    v_next BIGINT;
BEGIN
    INSERT INTO indicolors.order_payment_number_sequences (company_id, last_value)
    VALUES (p_company_id, 1)
    ON CONFLICT (company_id) DO UPDATE
        SET last_value = indicolors.order_payment_number_sequences.last_value + 1
    RETURNING last_value INTO v_next;

    RETURN 'ABN-' || v_next;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION indicolors.fn_next_abonos_number(CHARACTER VARYING) IS
    'Reserva el siguiente ABN-{n} de order_payment_number_sequences para abonos_number (agregado). '
    'Misma secuencia que payment_number: el id del agregado nunca choca con un movimiento.';


-- ============================================
-- 41. CREAR TABLA CUENTAS POR COBRAR (derivada, se mantiene por trigger)
-- ============================================
-- Agregado derivado de cuentas por cobrar por OP (solo escrito por triggers).
-- Incluye vencimiento (due_date) para aging/alertas; listo para que FE fije due_date después.

CREATE TABLE indicolors.accounts_receivable (
    accounts_receivable_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id             CHARACTER VARYING(64)       NOT NULL,
    cxc_number             CHARACTER VARYING(32)       NOT NULL,
    abonos_number          CHARACTER VARYING(32)       NOT NULL,
    production_order_id    CHARACTER VARYING(64)       NOT NULL,
    client_id              CHARACTER VARYING(64)       NOT NULL,

    total_units            INTEGER                     NOT NULL DEFAULT 0,
    delivered_units        INTEGER                     NOT NULL DEFAULT 0,
    pending_units          INTEGER                     NOT NULL DEFAULT 0,

    total_owed             NUMERIC(14,2)               NOT NULL DEFAULT 0,
    total_paid             NUMERIC(14,2)               NOT NULL DEFAULT 0,
    total_remaining        NUMERIC(14,2)               NOT NULL DEFAULT 0,

    total_cash_paid        NUMERIC(14,2)               NOT NULL DEFAULT 0,
    total_withheld         NUMERIC(14,2)               NOT NULL DEFAULT 0,
    total_advance_paid     NUMERIC(14,2)               NOT NULL DEFAULT 0,

    opened_at              TIMESTAMP WITHOUT TIME ZONE,
    due_date               DATE,
    payment_term_days      INTEGER                     NOT NULL DEFAULT 0,

    status                 CHARACTER VARYING(16)       NOT NULL DEFAULT 'pendiente',
    last_delivery_at       TIMESTAMP WITHOUT TIME ZONE,
    last_payment_number    CHARACTER VARYING(32),
    last_payment_at        TIMESTAMP WITHOUT TIME ZONE,

    updated_at             TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT accounts_receivable_pkey PRIMARY KEY (accounts_receivable_id),
    CONSTRAINT accounts_receivable_production_order_unique UNIQUE (production_order_id),
    CONSTRAINT accounts_receivable_cxc_number_company_unique UNIQUE (company_id, cxc_number),
    CONSTRAINT accounts_receivable_abonos_number_company_unique UNIQUE (company_id, abonos_number),
    CONSTRAINT accounts_receivable_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT accounts_receivable_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT accounts_receivable_client_fk
        FOREIGN KEY (client_id) REFERENCES indicolors.clients (client_id),
    CONSTRAINT accounts_receivable_status_check
        CHECK (status IN ('pendiente', 'parcial', 'pagado', 'anulado')),
    CONSTRAINT accounts_receivable_units_check
        CHECK (total_units >= 0 AND delivered_units >= 0 AND pending_units >= 0),
    CONSTRAINT accounts_receivable_payment_term_days_check
        CHECK (payment_term_days >= 0),
    CONSTRAINT accounts_receivable_cxc_number_format_check
        CHECK (cxc_number ~ '^CXC-[0-9]+$'),
    CONSTRAINT accounts_receivable_abonos_number_format_check
        CHECK (abonos_number ~ '^ABN-[0-9]+$')
);

CREATE INDEX idx_accounts_receivable_company ON indicolors.accounts_receivable (company_id, status);
CREATE INDEX idx_accounts_receivable_client ON indicolors.accounts_receivable (company_id, client_id);
CREATE INDEX idx_accounts_receivable_cxc_number ON indicolors.accounts_receivable (company_id, cxc_number);
CREATE INDEX idx_accounts_receivable_abonos_number ON indicolors.accounts_receivable (company_id, abonos_number);
CREATE INDEX idx_accounts_receivable_due_date
    ON indicolors.accounts_receivable (company_id, due_date)
    WHERE total_remaining > 0 AND due_date IS NOT NULL;

COMMENT ON TABLE indicolors.accounts_receivable IS
    'Agregado por OP para CxC: saldos, desglose de liquidación y vencimiento. Solo triggers; nunca edición manual desde backend';

COMMENT ON COLUMN indicolors.accounts_receivable.accounts_receivable_id IS 'Identificador único (UUID) de la Cuenta por cobrar';
COMMENT ON COLUMN indicolors.accounts_receivable.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.accounts_receivable.cxc_number IS 'Consecutivo CXC-{n}; lo asigna el trigger en el primer INSERT';
COMMENT ON COLUMN indicolors.accounts_receivable.abonos_number IS
    'Id de negocio del agregado de Abonos (ABN-{n}). 1:1 con la OP. Inmutable. '
    'Misma familia ABN- que payment_number, pero valor distinto (secuencia compartida). ≠ last_payment_number.';
COMMENT ON COLUMN indicolors.accounts_receivable.production_order_id IS 'OP asociada (1:1)';
COMMENT ON COLUMN indicolors.accounts_receivable.client_id IS 'Cliente de la OP';
COMMENT ON COLUMN indicolors.accounts_receivable.total_units IS 'Unidades totales de la OP (snapshot requested_quantity)';
COMMENT ON COLUMN indicolors.accounts_receivable.delivered_units IS 'Unidades entregadas netas';
COMMENT ON COLUMN indicolors.accounts_receivable.pending_units IS 'total_units - delivered_units, nunca negativo';
COMMENT ON COLUMN indicolors.accounts_receivable.total_owed IS
    'Valor acumulado de lo entregado (cartera CxC por entregas). API Abonos expone totalToCharge de la OP en totalOwed.';
COMMENT ON COLUMN indicolors.accounts_receivable.total_paid IS 'Suma neta de liquidaciones (abono+anticipo+retencion - reversiones)';
COMMENT ON COLUMN indicolors.accounts_receivable.total_remaining IS
    'En BD: total_owed(entregas) - total_paid. API Abonos: totalToCharge(OP) - total_paid (puede ser negativo).';
COMMENT ON COLUMN indicolors.accounts_receivable.total_cash_paid IS 'Suma neta de abonos en caja (abono - reversiones de abono)';
COMMENT ON COLUMN indicolors.accounts_receivable.total_withheld IS 'Suma neta de retenciones sufridas';
COMMENT ON COLUMN indicolors.accounts_receivable.total_advance_paid IS 'Suma neta de anticipos aplicados/registrados';
COMMENT ON COLUMN indicolors.accounts_receivable.opened_at IS
    'Fecha/hora de la primera entrega que abrió la CxC (delivered_at). Se fija una sola vez; no se actualiza con entregas posteriores ni se limpia al revertir (valor histórico).';
COMMENT ON COLUMN indicolors.accounts_receivable.due_date IS
    'Fecha límite de pago (opened_at::date + payment_term_days). FE podrá actualizarla al emitir factura';
COMMENT ON COLUMN indicolors.accounts_receivable.payment_term_days IS
    'Días de crédito snapshot desde clients.credit_days al abrir la CxC';
COMMENT ON COLUMN indicolors.accounts_receivable.status IS
    'pendiente|parcial|pagado|anulado (estado de liquidación; el aging se calcula aparte con due_date)';
COMMENT ON COLUMN indicolors.accounts_receivable.last_delivery_at IS 'delivered_at de la última entrega';
COMMENT ON COLUMN indicolors.accounts_receivable.last_payment_number IS
    'Último payment_number (ABN-{n}) vigente de la OP. Null si no hay liquidaciones netas. '
    'No es el id del agregado de Abonos (ese es abonos_number = ABN-{n} distinto).';
COMMENT ON COLUMN indicolors.accounts_receivable.last_payment_at IS
    'paid_at del último abono/anticipo/retención vigente (no reversión). Null si no hay liquidaciones netas.';
COMMENT ON COLUMN indicolors.accounts_receivable.updated_at IS 'Última actualización del agregado';

GRANT ALL PRIVILEGES ON TABLE indicolors.accounts_receivable TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.accounts_receivable TO indicolors_app;

-- ============================================
-- 42-44. TRIGGERS CxC / Abonos (validación entregas + sync entrega/pago)
-- ============================================
-- Triggers del módulo Pedidos / CxC / Abonos (sin CREATE TABLE).
-- Estrategia CXC/Abonos: el trigger asigna accounts_receivable_id (DEFAULT) + cxc_number vía fn_next_cxc_number
-- y abonos_number vía fn_next_abonos_number (ABN-{n}; misma secuencia que payment_number → ≠ movimiento)
-- solo en el primer INSERT; los UPDATE posteriores nunca regeneran esos campos.
-- due_date / payment_term_days se fijan al abrir deuda (1ª entrega) desde clients.credit_days.
-- API Abonos: totalOwed/totalRemaining se calculan en lectura como totalToCharge(OP) − totalPaid
-- (DB total_owed sigue siendo valor acumulado de entregas para cartera CxC por entregas).
-- Depende de: order_deliveries, order_payments, accounts_receivable, station_order_progress,
-- clients, accounts_receivable_number_sequences / fn_next_cxc_number,
-- order_payment_number_sequences / fn_next_abonos_number (V52).

CREATE OR REPLACE FUNCTION indicolors.fn_validate_delivery()
RETURNS TRIGGER AS $$
DECLARE
    v_processed     INTEGER;
    v_delivered     INTEGER;
    v_available     INTEGER;
    v_orig          indicolors.order_deliveries%ROWTYPE;
BEGIN
    IF NEW.movement_type = 'entrega' THEN
        SELECT cantidad_disponible INTO v_processed
        FROM indicolors.station_order_progress
        WHERE production_order_id = NEW.production_order_id;

        SELECT delivered_units INTO v_delivered
        FROM indicolors.accounts_receivable
        WHERE production_order_id = NEW.production_order_id;

        v_available := COALESCE(v_processed, 0) - COALESCE(v_delivered, 0);
        NEW.available_before := v_available;

        IF NEW.quantity_delivered > v_available THEN
            RAISE EXCEPTION
                'No se puede entregar % unidades: solo hay % disponibles para la OP %',
                NEW.quantity_delivered, v_available, NEW.production_order_id;
        END IF;
    ELSE
        SELECT * INTO v_orig
        FROM indicolors.order_deliveries
        WHERE order_delivery_id = NEW.reversed_delivery_id;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'La entrega a anular % no existe', NEW.reversed_delivery_id;
        END IF;

        IF v_orig.movement_type <> 'entrega' THEN
            RAISE EXCEPTION 'Solo se puede anular una entrega original, no otra reversión (%)', NEW.reversed_delivery_id;
        END IF;

        IF v_orig.production_order_id <> NEW.production_order_id
           OR v_orig.company_id <> NEW.company_id THEN
            RAISE EXCEPTION 'La reversión debe pertenecer a la misma OP y compañía que la entrega original %', NEW.reversed_delivery_id;
        END IF;

        IF EXISTS (
            SELECT 1 FROM indicolors.order_deliveries
            WHERE reversed_delivery_id = NEW.reversed_delivery_id
        ) THEN
            RAISE EXCEPTION 'La entrega % ya fue anulada previamente', NEW.reversed_delivery_id;
        END IF;

        IF NEW.quantity_delivered <> v_orig.quantity_delivered
           OR NEW.total_value <> v_orig.total_value THEN
            RAISE EXCEPTION
                'La reversión debe anular exactamente la entrega original: % unidades por %',
                v_orig.quantity_delivered, v_orig.total_value;
        END IF;

        -- Abonos se aplican sobre totalToCharge de la OP (API), no sobre valor entregado:
        -- no se valida total_paid vs total_owed de entregas al anular.

        SELECT cantidad_disponible INTO v_processed
        FROM indicolors.station_order_progress
        WHERE production_order_id = NEW.production_order_id;

        SELECT delivered_units INTO v_delivered
        FROM indicolors.accounts_receivable
        WHERE production_order_id = NEW.production_order_id;

        NEW.available_before := COALESCE(v_processed, 0) - COALESCE(v_delivered, 0);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_deliveries_validate ON indicolors.order_deliveries;
CREATE TRIGGER trg_deliveries_validate
    BEFORE INSERT ON indicolors.order_deliveries
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_validate_delivery();

COMMENT ON FUNCTION indicolors.fn_validate_delivery() IS
    'entrega: calcula available_before y rechaza si quantity_delivered supera lo disponible. reversion: valida entrega original (Abonos usa totalToCharge OP, no bloquea por total_paid vs valor entregado)';

CREATE OR REPLACE FUNCTION indicolors.fn_sync_accounts_receivable_delivery()
RETURNS TRIGGER AS $$
DECLARE
    v_total_units INTEGER;
    v_exists      BOOLEAN;
    v_sign        INTEGER;
    v_term_days   INTEGER;
BEGIN
    v_sign := CASE WHEN NEW.movement_type = 'reversion' THEN -1 ELSE 1 END;

    SELECT requested_quantity INTO v_total_units
    FROM indicolors.production_orders
    WHERE production_order_id = NEW.production_order_id;

    SELECT EXISTS (
        SELECT 1 FROM indicolors.accounts_receivable WHERE production_order_id = NEW.production_order_id
    ) INTO v_exists;

    IF NOT v_exists THEN
        SELECT COALESCE(credit_days, 0) INTO v_term_days
        FROM indicolors.clients
        WHERE client_id = NEW.client_id;

        INSERT INTO indicolors.accounts_receivable (
            cxc_number, abonos_number, company_id, production_order_id, client_id,
            total_units, delivered_units, pending_units,
            total_owed, total_paid, total_remaining,
            total_cash_paid, total_withheld, total_advance_paid,
            opened_at, due_date, payment_term_days,
            status, last_delivery_at, updated_at
        )
        VALUES (
            indicolors.fn_next_cxc_number(NEW.company_id),
            indicolors.fn_next_abonos_number(NEW.company_id),
            NEW.company_id, NEW.production_order_id, NEW.client_id,
            COALESCE(v_total_units, NEW.quantity_delivered),
            NEW.quantity_delivered,
            GREATEST(COALESCE(v_total_units, NEW.quantity_delivered) - NEW.quantity_delivered, 0),
            NEW.total_value, 0, NEW.total_value,
            0, 0, 0,
            NEW.delivered_at,
            (NEW.delivered_at::date + COALESCE(v_term_days, 0)),
            COALESCE(v_term_days, 0),
            'pendiente', NEW.delivered_at, now()
        );
    ELSE
        UPDATE indicolors.accounts_receivable SET
            delivered_units  = delivered_units + v_sign * NEW.quantity_delivered,
            pending_units    = GREATEST(total_units - (delivered_units + v_sign * NEW.quantity_delivered), 0),
            total_owed       = total_owed + v_sign * NEW.total_value,
            total_remaining  = (total_owed + v_sign * NEW.total_value) - total_paid,
            opened_at        = CASE
                WHEN opened_at IS NULL AND NEW.movement_type = 'entrega' THEN NEW.delivered_at
                ELSE opened_at
            END,
            due_date         = CASE
                WHEN due_date IS NULL AND NEW.movement_type = 'entrega' THEN
                    (NEW.delivered_at::date + payment_term_days)
                ELSE due_date
            END,
            last_delivery_at = CASE WHEN NEW.movement_type = 'entrega' THEN NEW.delivered_at ELSE last_delivery_at END,
            updated_at       = now(),
            status = CASE
                WHEN (delivered_units + v_sign * NEW.quantity_delivered) = 0
                     AND (total_owed + v_sign * NEW.total_value) = 0
                     AND total_paid = 0 THEN 'anulado'
                WHEN total_paid >= (total_owed + v_sign * NEW.total_value)
                     AND (total_owed + v_sign * NEW.total_value) > 0 THEN 'pagado'
                WHEN total_paid > 0 THEN 'parcial'
                ELSE 'pendiente'
            END
        WHERE production_order_id = NEW.production_order_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_deliveries_sync_accounts_receivable ON indicolors.order_deliveries;
CREATE TRIGGER trg_deliveries_sync_accounts_receivable
    AFTER INSERT ON indicolors.order_deliveries
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_sync_accounts_receivable_delivery();

COMMENT ON FUNCTION indicolors.fn_sync_accounts_receivable_delivery() IS
    'Upsert CxC tras entrega/reversión; fija opened_at/due_date/payment_term_days solo en apertura (opened_at nunca se reescribe ni se limpia)';

CREATE OR REPLACE FUNCTION indicolors.fn_sync_accounts_receivable_payment()
RETURNS TRIGGER AS $$
DECLARE
    v_signed_amount         NUMERIC(14,2);
    v_exists                BOOLEAN;
    v_orig_type             CHARACTER VARYING(16);
    v_bucket_type           CHARACTER VARYING(16);
    v_cash_delta            NUMERIC(14,2) := 0;
    v_withheld_delta        NUMERIC(14,2) := 0;
    v_advance_delta         NUMERIC(14,2) := 0;
    v_term_days             INTEGER;
    v_last_payment_number   CHARACTER VARYING(32);
    v_last_payment_at       TIMESTAMP WITHOUT TIME ZONE;
BEGIN
    v_signed_amount := CASE WHEN NEW.payment_type = 'reversion' THEN -NEW.amount ELSE NEW.amount END;

    IF NEW.payment_type = 'reversion' THEN
        SELECT payment_type INTO v_orig_type
        FROM indicolors.order_payments
        WHERE order_payment_id = NEW.reversed_payment_id;
        v_bucket_type := COALESCE(v_orig_type, 'abono');

        SELECT p.payment_number, p.paid_at
        INTO v_last_payment_number, v_last_payment_at
        FROM indicolors.order_payments p
        WHERE p.production_order_id = NEW.production_order_id
          AND p.payment_type <> 'reversion'
          AND NOT EXISTS (
              SELECT 1
              FROM indicolors.order_payments r
              WHERE r.reversed_payment_id = p.order_payment_id
                AND r.payment_type = 'reversion'
          )
        ORDER BY p.paid_at DESC, p.created_at DESC
        LIMIT 1;
    ELSE
        v_bucket_type := NEW.payment_type;
        v_last_payment_number := NEW.payment_number;
        v_last_payment_at := NEW.paid_at;
    END IF;

    IF v_bucket_type = 'retencion' THEN
        v_withheld_delta := v_signed_amount;
    ELSIF v_bucket_type = 'anticipo' THEN
        v_advance_delta := v_signed_amount;
    ELSE
        v_cash_delta := v_signed_amount;
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM indicolors.accounts_receivable WHERE production_order_id = NEW.production_order_id
    ) INTO v_exists;

    IF NOT v_exists THEN
        SELECT COALESCE(credit_days, 0) INTO v_term_days
        FROM indicolors.clients
        WHERE client_id = NEW.client_id;

        INSERT INTO indicolors.accounts_receivable (
            cxc_number, abonos_number, company_id, production_order_id, client_id,
            total_units, delivered_units, pending_units,
            total_owed, total_paid, total_remaining,
            total_cash_paid, total_withheld, total_advance_paid,
            opened_at, due_date, payment_term_days,
            status, last_payment_number, last_payment_at, updated_at
        )
        VALUES (
            indicolors.fn_next_cxc_number(NEW.company_id),
            indicolors.fn_next_abonos_number(NEW.company_id),
            NEW.company_id, NEW.production_order_id, NEW.client_id,
            0, 0, 0, 0, v_signed_amount, -v_signed_amount,
            v_cash_delta, v_withheld_delta, v_advance_delta,
            NULL, NULL, COALESCE(v_term_days, 0),
            'pendiente', v_last_payment_number, v_last_payment_at, now()
        );
    ELSE
        UPDATE indicolors.accounts_receivable SET
            total_paid           = total_paid + v_signed_amount,
            total_remaining      = total_owed - (total_paid + v_signed_amount),
            total_cash_paid      = total_cash_paid + v_cash_delta,
            total_withheld       = total_withheld + v_withheld_delta,
            total_advance_paid   = total_advance_paid + v_advance_delta,
            last_payment_number  = v_last_payment_number,
            last_payment_at      = v_last_payment_at,
            updated_at           = now(),
            status = CASE
                WHEN delivered_units = 0 AND total_owed = 0
                     AND (total_paid + v_signed_amount) = 0 THEN 'anulado'
                WHEN total_owed > 0
                     AND (total_paid + v_signed_amount) >= total_owed THEN 'pagado'
                WHEN (total_paid + v_signed_amount) > 0 THEN 'parcial'
                ELSE 'pendiente'
            END
        WHERE production_order_id = NEW.production_order_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_payments_sync_accounts_receivable ON indicolors.order_payments;
CREATE TRIGGER trg_payments_sync_accounts_receivable
    AFTER INSERT ON indicolors.order_payments
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_sync_accounts_receivable_payment();

COMMENT ON FUNCTION indicolors.fn_sync_accounts_receivable_payment() IS
    'Upsert CxC tras abono/anticipo/retencion/reversion; actualiza total_paid, desglose y last_payment_* del último vigente';

-- Backfills defensivos (idempotentes) del agregado CxC; viven aquí (no en V46) para no mezclar DML con el DDL de la tabla.
UPDATE indicolors.accounts_receivable ar
SET opened_at = src.first_delivered_at
FROM (
    SELECT
        d.production_order_id,
        MIN(d.delivered_at) AS first_delivered_at
    FROM indicolors.order_deliveries d
    WHERE d.movement_type = 'entrega'
      AND NOT EXISTS (
          SELECT 1
          FROM indicolors.order_deliveries r
          WHERE r.reversed_delivery_id = d.order_delivery_id
            AND r.movement_type = 'reversion'
      )
    GROUP BY d.production_order_id
) src
WHERE ar.production_order_id = src.production_order_id
  AND ar.opened_at IS NULL;

UPDATE indicolors.accounts_receivable ar
SET
    last_payment_number = src.payment_number,
    last_payment_at     = src.paid_at
FROM (
    SELECT DISTINCT ON (p.production_order_id)
        p.production_order_id,
        p.payment_number,
        p.paid_at
    FROM indicolors.order_payments p
    WHERE p.payment_type <> 'reversion'
      AND NOT EXISTS (
          SELECT 1
          FROM indicolors.order_payments r
          WHERE r.reversed_payment_id = p.order_payment_id
            AND r.payment_type = 'reversion'
      )
    ORDER BY p.production_order_id, p.paid_at DESC, p.created_at DESC
) src
WHERE ar.production_order_id = src.production_order_id;

UPDATE indicolors.accounts_receivable ar
SET
    last_payment_number = NULL,
    last_payment_at     = NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM indicolors.order_payments p
    WHERE p.production_order_id = ar.production_order_id
      AND p.payment_type <> 'reversion'
      AND NOT EXISTS (
          SELECT 1
          FROM indicolors.order_payments r
          WHERE r.reversed_payment_id = p.order_payment_id
            AND r.payment_type = 'reversion'
      )
);

-- Backfill abonos_number (ABN-{n}) para filas históricas sin cuenta de Abonos.
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT accounts_receivable_id, company_id
        FROM indicolors.accounts_receivable
        WHERE abonos_number IS NULL
           OR abonos_number = ''
        ORDER BY company_id, opened_at NULLS LAST, accounts_receivable_id
    LOOP
        UPDATE indicolors.accounts_receivable
        SET abonos_number = indicolors.fn_next_abonos_number(r.company_id)
        WHERE accounts_receivable_id = r.accounts_receivable_id;
    END LOOP;
END $$;

