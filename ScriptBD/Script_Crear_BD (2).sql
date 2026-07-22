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

