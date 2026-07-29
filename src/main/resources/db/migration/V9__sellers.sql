-- Tabla sellers (formulario "Nuevo vendedor") — un archivo por tabla, DDL completo.

CREATE TABLE IF NOT EXISTS indicolors.sellers (
    seller_id      VARCHAR(64)  NOT NULL DEFAULT gen_random_uuid()::text,
    company_id     VARCHAR(64)  NOT NULL,
    full_name      VARCHAR(200) NOT NULL,
    document_type  VARCHAR(20)  NOT NULL,
    identification VARCHAR(32)  NOT NULL,
    email          VARCHAR(320) NOT NULL,
    phone          VARCHAR(32),
    department     VARCHAR(100) NOT NULL,
    city           VARCHAR(120) NOT NULL,
    address        VARCHAR(255),
    state          BOOLEAN      NOT NULL DEFAULT TRUE,
    creation_date  DATE         NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT sellers_pkey PRIMARY KEY (seller_id),
    CONSTRAINT sellers_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT sellers_identification_company_unique
        UNIQUE (company_id, identification),
    CONSTRAINT sellers_document_type_check
        CHECK (document_type IN ('CC', 'CE', 'TI', 'PA', 'NIT')),
    CONSTRAINT sellers_email_check
        CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

CREATE INDEX IF NOT EXISTS idx_sellers_company_id ON indicolors.sellers (company_id);
CREATE INDEX IF NOT EXISTS idx_sellers_full_name ON indicolors.sellers (full_name);
CREATE INDEX IF NOT EXISTS idx_sellers_document ON indicolors.sellers (document_type, identification);
CREATE INDEX IF NOT EXISTS idx_sellers_department_city ON indicolors.sellers (department, city);
CREATE INDEX IF NOT EXISTS idx_sellers_state ON indicolors.sellers (state);
CREATE INDEX IF NOT EXISTS idx_sellers_company_state ON indicolors.sellers (company_id, state);
CREATE INDEX IF NOT EXISTS idx_sellers_email_lower ON indicolors.sellers (LOWER(email));

COMMENT ON TABLE indicolors.sellers IS 'Tabla de vendedores registrados por cada compañía (formulario Nuevo vendedor)';
COMMENT ON COLUMN indicolors.sellers.seller_id IS 'Identificador único del vendedor';
COMMENT ON COLUMN indicolors.sellers.company_id IS 'Identificador de la empresa dueña del registro del vendedor';
COMMENT ON COLUMN indicolors.sellers.full_name IS 'Nombre completo del vendedor';
COMMENT ON COLUMN indicolors.sellers.document_type IS 'Tipo de documento: CC, CE, TI, PA, NIT';
COMMENT ON COLUMN indicolors.sellers.identification IS 'Número de identificación del vendedor';
COMMENT ON COLUMN indicolors.sellers.email IS 'Correo electrónico del vendedor';
COMMENT ON COLUMN indicolors.sellers.phone IS 'Teléfono / contacto del vendedor';
COMMENT ON COLUMN indicolors.sellers.department IS 'Departamento de ubicación';
COMMENT ON COLUMN indicolors.sellers.city IS 'Ciudad/municipio de ubicación';
COMMENT ON COLUMN indicolors.sellers.address IS 'Dirección (calle, barrio, referencia)';
COMMENT ON COLUMN indicolors.sellers.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.sellers.creation_date IS 'Fecha de registro del vendedor en el sistema';
