-- ============================================
-- 1. CREAR BASE DE DATOS
-- ============================================
CREATE DATABASE indicore
    WITH
    ENCODING = 'UTF8'
    LC_COLLATE = 'es_CO.UTF-8'
    LC_CTYPE = 'es_CO.UTF-8'
    TEMPLATE = template0;

-- ============================================
-- 2. CREAR ROL/USUARIO
-- ============================================
CREATE ROLE indicore_admin WITH
    LOGIN
    SUPERUSER
    CREATEDB
    CREATEROLE
    PASSWORD 'indicore2026!';

CREATE ROLE indicore_app WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    PASSWORD 'indicolors2026!';

GRANT CONNECT ON DATABASE nombre_basedatos TO indicore_app;

-- ============================================
-- 3. CONECTAR A LA BASE DE DATOS
-- ============================================
\connect indicore;


-- 4. CREAR SCHEMA
CREATE SCHEMA IF NOT EXISTS indicolors
    AUTHORIZATION indicore_admin;

-- 5. PERMISOS BASE DE DATOS
GRANT ALL PRIVILEGES ON DATABASE indicore TO indicore_admin;
GRANT CONNECT ON DATABASE indicore TO indicore_app;
GRANT USAGE, CREATE ON SCHEMA indicolors TO indicore_app;

-- 6. PERMISOS SCHEMA
GRANT ALL PRIVILEGES ON SCHEMA indicolors TO indicore_admin;
GRANT USAGE ON SCHEMA indicolors TO indicore_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA indicolors
    GRANT ALL PRIVILEGES ON TABLES TO indicore_admin;

ALTER DEFAULT PRIVILEGES IN SCHEMA indicolors
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO indicore_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA indicolors
    GRANT ALL PRIVILEGES ON SEQUENCES TO indicore_admin;

ALTER DEFAULT PRIVILEGES IN SCHEMA indicolors
    GRANT USAGE, SELECT ON SEQUENCES TO indicore_app;

-- 7. SEARCH PATH
ALTER DATABASE indicore SET search_path TO indicolors, public;

-- 8. CREAR TABLA
CREATE TABLE indicolors.users (
    user_id               CHARACTER VARYING(64)        NOT NULL,
    company_id            CHARACTER VARYING(64)        NOT NULL,
    identification_number CHARACTER VARYING(64)        NOT NULL,
    document_type         CHARACTER VARYING(20)        NOT NULL,
    name                  CHARACTER VARYING(200)       NOT NULL,
    mail                  CHARACTER VARYING(320)       NOT NULL,
    contact               CHARACTER VARYING(300)       NOT NULL,
    address               CHARACTER VARYING(255)       NOT NULL,
    password_hash         CHARACTER VARYING(200)       NOT NULL,
    creation_date         DATE                         NOT NULL,
    state                 BOOLEAN                      NOT NULL DEFAULT TRUE,
    token_version         BIGINT,
    role_id               UUID,
    force_password_change BOOLEAN                      DEFAULT TRUE,
    password_changed_at   TIMESTAMP WITHOUT TIME ZONE,
    password_expires_at   TIMESTAMP WITHOUT TIME ZONE,
    failed_attempts       INTEGER                      DEFAULT 0,
    locked_until          TIMESTAMP WITHOUT TIME ZONE,
    last_login_at         TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT users_pkey PRIMARY KEY (user_id),
    CONSTRAINT users_mail_unique UNIQUE (mail),
    CONSTRAINT users_identification_unique UNIQUE (identification_number),
    CONSTRAINT users_failed_attempts_check CHECK (failed_attempts >= 0)
);

-- 9. ÍNDICES
CREATE INDEX idx_users_company_id ON indicolors.users (company_id);
CREATE INDEX idx_users_mail ON indicolors.users (mail);
CREATE INDEX idx_users_state ON indicolors.users (state);
CREATE INDEX idx_users_role_id ON indicolors.users (role_id);
CREATE INDEX idx_users_document ON indicolors.users (document_type, identification_number);

-- 10. COMENTARIOS
COMMENT ON TABLE indicolors.users IS 'Tabla de usuarios del sistema';
COMMENT ON COLUMN indicolors.users.user_id IS 'Identificador único del usuario';
COMMENT ON COLUMN indicolors.users.company_id IS 'Identificador de la empresa';
COMMENT ON COLUMN indicolors.users.document_type IS 'Tipo de documento: CC, CE, TI, PA, NIT';
COMMENT ON COLUMN indicolors.users.mail IS 'Correo electrónico único del usuario';
COMMENT ON COLUMN indicolors.users.password_hash IS 'Hash de la contraseña';
COMMENT ON COLUMN indicolors.users.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.users.failed_attempts IS 'Intentos fallidos de login';
COMMENT ON COLUMN indicolors.users.locked_until IS 'Fecha hasta la que está bloqueado el usuario';

-- 11. PERMISOS TABLA
GRANT ALL PRIVILEGES ON TABLE indicolors.users TO indicore_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.users TO indicore_app;