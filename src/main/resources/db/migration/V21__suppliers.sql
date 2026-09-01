-- Tabla suppliers (formulario "Nuevo proveedor") — un archivo por tabla, DDL completo.

CREATE TABLE IF NOT EXISTS indicolors.suppliers (
    supplier_id    VARCHAR(64)  NOT NULL DEFAULT gen_random_uuid()::text,
    company_id     VARCHAR(64)  NOT NULL,
    name           VARCHAR(200) NOT NULL,
    document_type  VARCHAR(20),
    identification VARCHAR(32),
    department     VARCHAR(100) NOT NULL,
    city           VARCHAR(120) NOT NULL,
    address        VARCHAR(255),
    phone          VARCHAR(32),
    email          VARCHAR(320),
    contact_person VARCHAR(200),
    state          BOOLEAN      NOT NULL DEFAULT TRUE,
    creation_date  DATE         NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT suppliers_pkey PRIMARY KEY (supplier_id),
    CONSTRAINT suppliers_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT suppliers_document_type_check
        CHECK (document_type IS NULL OR document_type IN ('CC', 'CE', 'TI', 'PA', 'NIT')),
    CONSTRAINT suppliers_email_check
        CHECK (email IS NULL OR email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

CREATE INDEX IF NOT EXISTS idx_suppliers_company_id ON indicolors.suppliers (company_id);
CREATE INDEX IF NOT EXISTS idx_suppliers_name ON indicolors.suppliers (name);
CREATE INDEX IF NOT EXISTS idx_suppliers_identification ON indicolors.suppliers (identification);
CREATE INDEX IF NOT EXISTS idx_suppliers_document ON indicolors.suppliers (document_type, identification);
CREATE INDEX IF NOT EXISTS idx_suppliers_department_city ON indicolors.suppliers (department, city);
CREATE INDEX IF NOT EXISTS idx_suppliers_state ON indicolors.suppliers (state);
CREATE INDEX IF NOT EXISTS idx_suppliers_company_state ON indicolors.suppliers (company_id, state);

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
