-- Tabla assembly_prices (formulario "Nuevo precio de montaje").

CREATE TABLE IF NOT EXISTS indicolors.assembly_prices (
    assembly_price_id VARCHAR(64)     NOT NULL DEFAULT gen_random_uuid()::text,
    company_id        VARCHAR(64)     NOT NULL,
    name              VARCHAR(150)    NOT NULL,
    cost              NUMERIC(12,2)   NOT NULL,
    state             BOOLEAN         NOT NULL DEFAULT TRUE,
    creation_date     DATE            NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT assembly_prices_pkey PRIMARY KEY (assembly_price_id),
    CONSTRAINT assembly_prices_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT assembly_prices_name_company_unique UNIQUE (company_id, name),
    CONSTRAINT assembly_prices_cost_check CHECK (cost >= 0)
);

CREATE INDEX IF NOT EXISTS idx_assembly_prices_company_id ON indicolors.assembly_prices (company_id);
CREATE INDEX IF NOT EXISTS idx_assembly_prices_name ON indicolors.assembly_prices (name);
CREATE INDEX IF NOT EXISTS idx_assembly_prices_state ON indicolors.assembly_prices (state);
CREATE INDEX IF NOT EXISTS idx_assembly_prices_company_state ON indicolors.assembly_prices (company_id, state);

COMMENT ON TABLE indicolors.assembly_prices IS 'Catálogo de Precios de montaje (ej. Montaje estándar 4 tintas), usados al configurar órdenes de producción';
COMMENT ON COLUMN indicolors.assembly_prices.assembly_price_id IS 'Identificador único del precio de montaje';
COMMENT ON COLUMN indicolors.assembly_prices.company_id IS 'Identificador de la empresa dueña del precio de montaje';
COMMENT ON COLUMN indicolors.assembly_prices.name IS 'Nombre del precio de montaje (ej. Montaje estándar 4 tintas)';
COMMENT ON COLUMN indicolors.assembly_prices.cost IS 'Costo del montaje';
COMMENT ON COLUMN indicolors.assembly_prices.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.assembly_prices.creation_date IS 'Fecha de registro del precio de montaje en el sistema';

GRANT ALL PRIVILEGES ON TABLE indicolors.assembly_prices TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.assembly_prices TO indicolors_app;

-- Datos semilla demo (company-seed-001). Nombres con U&... por encoding.
INSERT INTO indicolors.assembly_prices (
    assembly_price_id, company_id, name, cost, state, creation_date
)
SELECT v.assembly_price_id, v.company_id, v.name, v.cost, v.state, v.creation_date
FROM (VALUES
    ('assembly-price-seed-001', 'company-seed-001', U&'Montaje est\00E1ndar 4 tintas', 85000.00::numeric, TRUE, CURRENT_DATE),
    ('assembly-price-seed-002', 'company-seed-001', 'Montaje complejo 6 tintas', 125000.00::numeric, TRUE, CURRENT_DATE),
    ('assembly-price-seed-003', 'company-seed-001', U&'Montaje econ\00F3mico 2 tintas', 55000.00::numeric, TRUE, CURRENT_DATE)
) AS v(assembly_price_id, company_id, name, cost, state, creation_date)
WHERE NOT EXISTS (
    SELECT 1 FROM indicolors.assembly_prices a
    WHERE a.assembly_price_id = v.assembly_price_id
       OR (a.company_id = v.company_id AND lower(trim(a.name)) = lower(trim(v.name)))
);
