-- Tabla thousand_rates (formulario "Nueva tarifa por millar").

CREATE TABLE IF NOT EXISTS indicolors.thousand_rates (
    thousand_rate_id    VARCHAR(64)     NOT NULL DEFAULT gen_random_uuid()::text,
    company_id          VARCHAR(64)     NOT NULL,
    name                VARCHAR(150)    NOT NULL,
    color_category      VARCHAR(50)     NOT NULL,
    thousand_unit       INTEGER         NOT NULL DEFAULT 1000,
    price               NUMERIC(12,2)   NOT NULL,
    state               BOOLEAN         NOT NULL DEFAULT TRUE,
    min_threshold_units INTEGER         NOT NULL,
    min_thousand        NUMERIC(10,2)   NOT NULL,
    decimal_threshold   NUMERIC(3,2)    NOT NULL,
    gripper_flip_price  NUMERIC(12,2),
    square_flip_price   NUMERIC(12,2),
    is_default          BOOLEAN         NOT NULL DEFAULT FALSE,
    creation_date       DATE            NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT thousand_rates_pkey PRIMARY KEY (thousand_rate_id),
    CONSTRAINT thousand_rates_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT thousand_rates_name_company_unique UNIQUE (company_id, name),
    CONSTRAINT thousand_rates_thousand_unit_check CHECK (thousand_unit > 0),
    CONSTRAINT thousand_rates_price_check CHECK (price >= 0),
    CONSTRAINT thousand_rates_min_threshold_units_check CHECK (min_threshold_units > 0),
    CONSTRAINT thousand_rates_min_thousand_check CHECK (min_thousand > 0),
    CONSTRAINT thousand_rates_decimal_threshold_check CHECK (decimal_threshold >= 0 AND decimal_threshold <= 1),
    CONSTRAINT thousand_rates_gripper_flip_price_check CHECK (gripper_flip_price IS NULL OR gripper_flip_price >= 0),
    CONSTRAINT thousand_rates_square_flip_price_check CHECK (square_flip_price IS NULL OR square_flip_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_thousand_rates_company_id ON indicolors.thousand_rates (company_id);
CREATE INDEX IF NOT EXISTS idx_thousand_rates_name ON indicolors.thousand_rates (name);
CREATE INDEX IF NOT EXISTS idx_thousand_rates_state ON indicolors.thousand_rates (state);
CREATE INDEX IF NOT EXISTS idx_thousand_rates_company_state ON indicolors.thousand_rates (company_id, state);
CREATE INDEX IF NOT EXISTS idx_thousand_rates_color_category ON indicolors.thousand_rates (color_category);
CREATE UNIQUE INDEX IF NOT EXISTS idx_thousand_rates_one_default_per_company_color
    ON indicolors.thousand_rates (company_id, lower(trim(color_category)))
    WHERE is_default = TRUE;

COMMENT ON TABLE indicolors.thousand_rates IS 'Catálogo de Tarifas por millar (ej. Color básico), con reglas de millar y volteo, usadas al configurar órdenes de producción';
COMMENT ON COLUMN indicolors.thousand_rates.thousand_rate_id IS 'Identificador único de la tarifa por millar';
COMMENT ON COLUMN indicolors.thousand_rates.company_id IS 'Identificador de la empresa dueña de la tarifa';
COMMENT ON COLUMN indicolors.thousand_rates.name IS 'Nombre de la tarifa (ej. Color básico)';
COMMENT ON COLUMN indicolors.thousand_rates.color_category IS 'Categoría de color de la tarifa (ej. 1 COLOR, 2 COLORES)';
COMMENT ON COLUMN indicolors.thousand_rates.thousand_unit IS 'Unidad de millar sobre la que se calcula la tarifa (por defecto 1000)';
COMMENT ON COLUMN indicolors.thousand_rates.price IS 'Precio de la tarifa por millar';
COMMENT ON COLUMN indicolors.thousand_rates.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.thousand_rates.min_threshold_units IS 'Cantidad mínima en unidades para aplicar reglas de cobro por millar';
COMMENT ON COLUMN indicolors.thousand_rates.min_thousand IS 'Millar mínimo de venta asociado a la tarifa';
COMMENT ON COLUMN indicolors.thousand_rates.decimal_threshold IS 'Umbral decimal: si la parte decimal es mayor a este valor, sube al entero siguiente más cercano; en caso contrario, conserva solo la parte entera';
COMMENT ON COLUMN indicolors.thousand_rates.gripper_flip_price IS 'Precio por millar con volteo por pinza; NULL si no aplica';
COMMENT ON COLUMN indicolors.thousand_rates.square_flip_price IS 'Precio por millar con volteo por escuadra; NULL si no aplica';
COMMENT ON COLUMN indicolors.thousand_rates.is_default IS 'True=tarifa por defecto para la categoría/empresa; False=no';
COMMENT ON COLUMN indicolors.thousand_rates.creation_date IS 'Fecha de registro de la tarifa en el sistema';
COMMENT ON INDEX indicolors.idx_thousand_rates_one_default_per_company_color IS
    'Como máximo una tarifa por defecto por empresa y categoría de color';
