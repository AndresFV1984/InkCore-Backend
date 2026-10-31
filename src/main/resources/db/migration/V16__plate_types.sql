-- Tabla plate_types (formulario "Nuevo tipo de plancha").

CREATE TABLE IF NOT EXISTS indicolors.plate_types (
    plate_type_id   VARCHAR(64)     NOT NULL DEFAULT gen_random_uuid()::text,
    company_id      VARCHAR(64)     NOT NULL,
    name            VARCHAR(150)    NOT NULL,
    width           NUMERIC(10,2)   NOT NULL,
    height          NUMERIC(10,2)   NOT NULL,
    unit            VARCHAR(10)     NOT NULL DEFAULT 'cm',
    value           NUMERIC(12,2)   NOT NULL,
    state           BOOLEAN         NOT NULL DEFAULT TRUE,
    creation_date   DATE            NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT plate_types_pkey PRIMARY KEY (plate_type_id),
    CONSTRAINT plate_types_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT plate_types_name_company_unique UNIQUE (company_id, name),
    CONSTRAINT plate_types_width_check CHECK (width > 0),
    CONSTRAINT plate_types_height_check CHECK (height > 0),
    CONSTRAINT plate_types_unit_check CHECK (unit IN ('cm', 'mm', 'in')),
    CONSTRAINT plate_types_value_check CHECK (value >= 0)
);

CREATE INDEX IF NOT EXISTS idx_plate_types_company_id ON indicolors.plate_types (company_id);
CREATE INDEX IF NOT EXISTS idx_plate_types_name ON indicolors.plate_types (name);
CREATE INDEX IF NOT EXISTS idx_plate_types_state ON indicolors.plate_types (state);
CREATE INDEX IF NOT EXISTS idx_plate_types_company_state ON indicolors.plate_types (company_id, state);

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

GRANT ALL PRIVILEGES ON TABLE indicolors.plate_types TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.plate_types TO indicolors_app;

-- Datos semilla demo (company-seed-001). Nombres con U&... por encoding.
INSERT INTO indicolors.plate_types (
    plate_type_id, company_id, name, width, height, unit, value, state, creation_date
)
SELECT v.plate_type_id, v.company_id, v.name, v.width, v.height, v.unit, v.value, v.state, v.creation_date
FROM (VALUES
    ('plate-type-seed-001', 'company-seed-001', U&'Plancha est\00E1ndar', 70.00::numeric, 100.00::numeric, 'cm', 185000.00::numeric, TRUE, CURRENT_DATE),
    ('plate-type-seed-002', 'company-seed-001', 'Plancha media', 64.00::numeric, 90.00::numeric, 'cm', 152000.00::numeric, TRUE, CURRENT_DATE),
    ('plate-type-seed-003', 'company-seed-001', U&'Plancha peque\00F1a', 50.00::numeric, 70.00::numeric, 'cm', 98000.00::numeric, TRUE, CURRENT_DATE)
) AS v(plate_type_id, company_id, name, width, height, unit, value, state, creation_date)
WHERE NOT EXISTS (
    SELECT 1 FROM indicolors.plate_types p
    WHERE p.plate_type_id = v.plate_type_id
       OR (p.company_id = v.company_id AND lower(trim(p.name)) = lower(trim(v.name)))
);
