-- Tabla clients (formulario "Nuevo cliente") — un archivo por tabla, DDL completo.
-- Alineado con ScriptBD/Script_Crear_BD (document_type + check).

CREATE TABLE IF NOT EXISTS indicolors.clients (
    client_id      VARCHAR(64)  NOT NULL DEFAULT gen_random_uuid()::text,
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
    credit_days    INTEGER      NOT NULL DEFAULT 0,
    state          BOOLEAN      NOT NULL DEFAULT TRUE,
    creation_date  DATE         NOT NULL DEFAULT CURRENT_DATE,
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

CREATE INDEX IF NOT EXISTS idx_clients_company_id ON indicolors.clients (company_id);
CREATE INDEX IF NOT EXISTS idx_clients_name ON indicolors.clients (name);
CREATE INDEX IF NOT EXISTS idx_clients_identification ON indicolors.clients (identification);
CREATE INDEX IF NOT EXISTS idx_clients_document ON indicolors.clients (document_type, identification);
CREATE INDEX IF NOT EXISTS idx_clients_department_city ON indicolors.clients (department, city);
CREATE INDEX IF NOT EXISTS idx_clients_state ON indicolors.clients (state);
CREATE INDEX IF NOT EXISTS idx_clients_company_state ON indicolors.clients (company_id, state);

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

-- Datos semilla demo (company-seed-001)
INSERT INTO indicolors.clients (
    client_id,
    company_id,
    name,
    document_type,
    identification,
    department,
    city,
    address,
    phone,
    email,
    contact_person,
    state,
    creation_date
) VALUES
    (
        'client-seed-001',
        'company-seed-001',
        'Editorial Horizonte Ltda.',
        'NIT',
        '800222001-1',
        'Antioquia',
        'Medellín',
        'Calle 10 # 43-20',
        '6042220001',
        'compras@editorialhorizonte.com',
        'María López',
        TRUE,
        CURRENT_DATE
    ),
    (
        'client-seed-002',
        'company-seed-001',
        'Agencia Visual Branding',
        'NIT',
        '800222002-2',
        'Antioquia',
        'Envigado',
        'Carrera 43A # 1 Sur-50',
        '6042220002',
        'produccion@visualbranding.com',
        'Andrés Mejía',
        TRUE,
        CURRENT_DATE
    ),
    (
        'client-seed-003',
        'company-seed-001',
        'Empaques Industriales del Norte',
        'NIT',
        '800222003-3',
        'Atlántico',
        'Barranquilla',
        'Calle 30 # 8-15',
        '6052220003',
        'ordenes@empaquesnorte.com',
        'Sofía Castro',
        TRUE,
        CURRENT_DATE
    )
ON CONFLICT (client_id) DO NOTHING;
