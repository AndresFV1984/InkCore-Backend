-- Tabla de compañías / empresas
CREATE TABLE IF NOT EXISTS indicolors.companies (
    company_id    VARCHAR(64)                   NOT NULL,
    name          VARCHAR(200)                  NOT NULL,
    nit           VARCHAR(32)                   NOT NULL,
    phone         VARCHAR(32),
    state         BOOLEAN                       NOT NULL DEFAULT TRUE,
    address       VARCHAR(255),
    city          VARCHAR(120),
    email         VARCHAR(320),
    creation_date DATE                          NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT companies_pkey PRIMARY KEY (company_id),
    CONSTRAINT companies_nit_unique UNIQUE (nit),
    CONSTRAINT companies_email_unique UNIQUE (email)
);

CREATE INDEX IF NOT EXISTS idx_companies_name ON indicolors.companies (name);
CREATE INDEX IF NOT EXISTS idx_companies_state ON indicolors.companies (state);
CREATE INDEX IF NOT EXISTS idx_companies_city ON indicolors.companies (city);

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

INSERT INTO indicolors.companies (
    company_id,
    name,
    nit,
    phone,
    state,
    address,
    city,
    email,
    creation_date
) VALUES (
    'company-seed-001',
    'InkCore S.A.S.',
    '900123456-7',
    '3001234567',
    TRUE,
    'Medellín, Colombia',
    'Medellín',
    'info@indicolors.com',
    CURRENT_DATE
)
ON CONFLICT (company_id) DO NOTHING;

