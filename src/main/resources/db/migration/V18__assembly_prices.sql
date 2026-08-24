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
