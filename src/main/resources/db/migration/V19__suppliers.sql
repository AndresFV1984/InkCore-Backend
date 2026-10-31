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

-- Datos semilla demo (company-seed-001)
INSERT INTO indicolors.suppliers (
    supplier_id, company_id, name, document_type, identification,
    department, city, address, phone, email, contact_person, state, creation_date
) VALUES
    (
        'supplier-seed-001', 'company-seed-001', 'Papeles del Oriente S.A.S.', 'NIT', '900111001-1',
        'Antioquia', 'Medellin', 'Calle 30 # 55-10, Zona Industrial', '6041110001',
        'ventas@papelesoriente.com', 'Laura Gomez', TRUE, CURRENT_DATE
    ),
    (
        'supplier-seed-002', 'company-seed-001', 'Insumos Graficos Andina', 'NIT', '900111002-2',
        'Antioquia', 'Itagui', 'Carrera 52 # 40-20', '6041110002',
        'contacto@insumosandina.com', 'Carlos Ruiz', TRUE, CURRENT_DATE
    ),
    (
        'supplier-seed-003', 'company-seed-001', 'Tintas y Quimicos Valle', 'NIT', '900111003-3',
        'Valle del Cauca', 'Cali', 'Av. 3N # 15-45', '6021110003',
        'comercial@tintasvalle.com', 'Ana Perez', TRUE, CURRENT_DATE
    )
ON CONFLICT (supplier_id) DO NOTHING;
