-- Usuarios (idempotente: compatible con Scrip Crear BD Indicore si la tabla ya existe)
CREATE TABLE IF NOT EXISTS indicolors.users (
    user_id               VARCHAR(64) NOT NULL,
    company_id            VARCHAR(64) NOT NULL,
    identification_number VARCHAR(64) NOT NULL,
    document_type         VARCHAR(20) NOT NULL,
    name                  VARCHAR(200) NOT NULL,
    mail                  VARCHAR(320) NOT NULL,
    contact               VARCHAR(300) NOT NULL,
    address               VARCHAR(255) NOT NULL,
    password_hash         VARCHAR(200) NOT NULL,
    creation_date         DATE NOT NULL,
    state                 BOOLEAN NOT NULL DEFAULT TRUE,
    token_version         BIGINT NOT NULL DEFAULT 1,
    role_id               UUID,
    force_password_change BOOLEAN DEFAULT TRUE,
    password_changed_at   TIMESTAMP WITHOUT TIME ZONE,
    password_expires_at   TIMESTAMP WITHOUT TIME ZONE,
    failed_attempts       INTEGER DEFAULT 0,
    locked_until          TIMESTAMP WITHOUT TIME ZONE,
    last_login_at         TIMESTAMP WITHOUT TIME ZONE,

    CONSTRAINT users_pkey PRIMARY KEY (user_id)
);

ALTER TABLE indicolors.users
    ALTER COLUMN token_version SET DEFAULT 1;

UPDATE indicolors.users
SET token_version = 1
WHERE token_version IS NULL;

ALTER TABLE indicolors.users
    ALTER COLUMN token_version SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'users_mail_unique' AND conrelid = 'indicolors.users'::regclass
    ) THEN
        ALTER TABLE indicolors.users ADD CONSTRAINT users_mail_unique UNIQUE (mail);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'users_identification_unique' AND conrelid = 'indicolors.users'::regclass
    ) THEN
        ALTER TABLE indicolors.users ADD CONSTRAINT users_identification_unique UNIQUE (identification_number);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'users_failed_attempts_check' AND conrelid = 'indicolors.users'::regclass
    ) THEN
        ALTER TABLE indicolors.users
            ADD CONSTRAINT users_failed_attempts_check CHECK (failed_attempts >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_users_role' AND conrelid = 'indicolors.users'::regclass
    ) THEN
        ALTER TABLE indicolors.users
            ADD CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES indicolors.roles (role_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_company_id ON indicolors.users (company_id);
CREATE INDEX IF NOT EXISTS idx_users_mail ON indicolors.users (mail);
CREATE INDEX IF NOT EXISTS idx_users_state ON indicolors.users (state);
CREATE INDEX IF NOT EXISTS idx_users_role_id ON indicolors.users (role_id);
CREATE INDEX IF NOT EXISTS idx_users_document ON indicolors.users (document_type, identification_number);

COMMENT ON TABLE indicolors.users IS 'Tabla de usuarios del sistema';
COMMENT ON COLUMN indicolors.users.token_version IS 'Versión de sesión para invalidar tokens previos (claim tv en JWT)';

INSERT INTO indicolors.users (
    user_id,
    company_id,
    identification_number,
    document_type,
    name,
    mail,
    contact,
    address,
    password_hash,
    creation_date,
    state,
    token_version,
    role_id,
    force_password_change,
    failed_attempts
) VALUES (
    'seed-cfg-9f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c',
    'company-seed-001',
    '9001234567',
    'NIT',
    'Administrador IndiColores',
    'admin@indicolors.com',
    '3001234567',
    'Medellín, Colombia',
    '$2a$10$WGGWmu2DjL2BnBljv70Gzu7TShx6GHdIu/.ivtnSklhahp1zWJwQi',
    CURRENT_DATE,
    TRUE,
    1,
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11'::uuid,
    FALSE,
    0
)
ON CONFLICT (user_id) DO NOTHING;
