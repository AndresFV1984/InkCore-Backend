-- Usuarios del sistema
CREATE TABLE IF NOT EXISTS indicolors.users (
    user_id               VARCHAR(64)                  NOT NULL,
    company_id            VARCHAR(64)                  NOT NULL,
    identification_number VARCHAR(64)                  NOT NULL,
    document_type         VARCHAR(20)                  NOT NULL,
    name                  VARCHAR(200)                 NOT NULL,
    mail                  VARCHAR(320)                 NOT NULL,
    contact               VARCHAR(300)                 NOT NULL DEFAULT '',
    department            VARCHAR(100)                 NOT NULL,
    city                  VARCHAR(100)                 NOT NULL,
    address               VARCHAR(255),
    password_hash         VARCHAR(200)                 NOT NULL,
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

CREATE INDEX IF NOT EXISTS idx_users_company_id ON indicolors.users (company_id);
CREATE INDEX IF NOT EXISTS idx_users_mail ON indicolors.users (mail);
CREATE INDEX IF NOT EXISTS idx_users_state ON indicolors.users (state);
CREATE INDEX IF NOT EXISTS idx_users_document ON indicolors.users (document_type, identification_number);
CREATE INDEX IF NOT EXISTS idx_users_department_city ON indicolors.users (department, city);

COMMENT ON TABLE indicolors.users IS 'Tabla de usuarios del sistema';
COMMENT ON COLUMN indicolors.users.department IS 'Departamento de ubicación del usuario';
COMMENT ON COLUMN indicolors.users.city IS 'Ciudad/municipio de ubicación del usuario';
COMMENT ON COLUMN indicolors.users.token_version IS 'Versión de sesión para invalidar tokens (claim tv en JWT)';
COMMENT ON COLUMN indicolors.users.state IS 'True=Activo, False=Inactivo';

-- Usuario semilla administrador (password: Indicore2026!)
INSERT INTO indicolors.users (
    user_id,
    company_id,
    identification_number,
    document_type,
    name,
    mail,
    contact,
    department,
    city,
    address,
    password_hash,
    creation_date,
    state,
    token_version,
    force_password_change,
    failed_attempts
) VALUES (
    'seed-cfg-9f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c',
    'company-seed-001',
    '9001234567',
    'NIT',
    'Administrador InkCore',
    'admin@indicolors.com',
    '3001234567',
    'Antioquia',
    'Medellín',
    'Medellín, Colombia',
    '$2a$10$WGGWmu2DjL2BnBljv70Gzu7TShx6GHdIu/.ivtnSklhahp1zWJwQi',
    CURRENT_DATE,
    TRUE,
    1,
    FALSE,
    0
)
ON CONFLICT (user_id) DO NOTHING;

