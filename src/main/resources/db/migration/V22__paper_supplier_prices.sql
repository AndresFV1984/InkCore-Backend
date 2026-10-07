CREATE TABLE indicolors.paper_supplier_prices (
    paper_supplier_price_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id              CHARACTER VARYING(64)       NOT NULL,
    paper_id                CHARACTER VARYING(64)       NOT NULL,
    supplier_id             CHARACTER VARYING(64)       NOT NULL,
    sheet_value             NUMERIC(12,2)               NOT NULL,
    package_unit            INTEGER                     NOT NULL,
    freight_per_sheet       NUMERIC(12,2)               NOT NULL DEFAULT 0,
    min_purchase_sheets     INTEGER,
    payment_days            INTEGER,
    delivery_days           INTEGER,
    price_date              DATE                        NOT NULL DEFAULT CURRENT_DATE,
    preferred               BOOLEAN                     NOT NULL DEFAULT FALSE,
    state                   BOOLEAN                     NOT NULL DEFAULT TRUE,
    landed_cost_per_sheet   NUMERIC(12,2) GENERATED ALWAYS AS (sheet_value + freight_per_sheet) STORED,
    created_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT paper_supplier_prices_pkey PRIMARY KEY (paper_supplier_price_id),
    CONSTRAINT paper_supplier_prices_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT paper_supplier_prices_paper_fk
        FOREIGN KEY (paper_id) REFERENCES indicolors.papers (paper_id) ON DELETE CASCADE,
    CONSTRAINT paper_supplier_prices_supplier_fk
        FOREIGN KEY (supplier_id) REFERENCES indicolors.suppliers (supplier_id),
    CONSTRAINT paper_supplier_prices_paper_supplier_unique UNIQUE (paper_id, supplier_id),
    CONSTRAINT paper_supplier_prices_sheet_value_check CHECK (sheet_value > 0),
    CONSTRAINT paper_supplier_prices_package_unit_check CHECK (package_unit > 0),
    CONSTRAINT paper_supplier_prices_freight_check CHECK (freight_per_sheet >= 0),
    CONSTRAINT paper_supplier_prices_min_purchase_sheets_check
        CHECK (min_purchase_sheets IS NULL OR min_purchase_sheets > 0),
    CONSTRAINT paper_supplier_prices_payment_days_check
        CHECK (payment_days IS NULL OR payment_days >= 0),
    CONSTRAINT paper_supplier_prices_delivery_days_check
        CHECK (delivery_days IS NULL OR delivery_days >= 0)
);

CREATE INDEX idx_paper_supplier_prices_company_id ON indicolors.paper_supplier_prices (company_id);
CREATE INDEX idx_paper_supplier_prices_paper_id ON indicolors.paper_supplier_prices (paper_id);
CREATE INDEX idx_paper_supplier_prices_supplier_id ON indicolors.paper_supplier_prices (supplier_id);
CREATE INDEX idx_paper_supplier_prices_state ON indicolors.paper_supplier_prices (state);
CREATE UNIQUE INDEX idx_paper_supplier_prices_preferred_unique
    ON indicolors.paper_supplier_prices (paper_id)
    WHERE preferred = TRUE;

COMMENT ON TABLE indicolors.paper_supplier_prices IS 'Precio vigente por papel y proveedor. El esmaltado vive en papers, no aquí';
COMMENT ON COLUMN indicolors.paper_supplier_prices.paper_supplier_price_id IS 'Identificador único del precio';
COMMENT ON COLUMN indicolors.paper_supplier_prices.paper_id IS 'Papel del catálogo (FK a papers)';
COMMENT ON COLUMN indicolors.paper_supplier_prices.supplier_id IS 'Proveedor (FK a suppliers)';
COMMENT ON COLUMN indicolors.paper_supplier_prices.sheet_value IS 'Precio por pliego';
COMMENT ON COLUMN indicolors.paper_supplier_prices.package_unit IS 'Cantidad de hojas/pliegos por unidad de empaque de este proveedor';
COMMENT ON COLUMN indicolors.paper_supplier_prices.freight_per_sheet IS 'Flete por pliego; default 0';
COMMENT ON COLUMN indicolors.paper_supplier_prices.min_purchase_sheets IS 'Mínimo de pliegos de compra; NULL si no aplica';
COMMENT ON COLUMN indicolors.paper_supplier_prices.payment_days IS 'Plazo de pago en días';
COMMENT ON COLUMN indicolors.paper_supplier_prices.delivery_days IS 'Días de entrega estimados';
COMMENT ON COLUMN indicolors.paper_supplier_prices.price_date IS 'Fecha de vigencia del precio';
COMMENT ON COLUMN indicolors.paper_supplier_prices.preferred IS 'True=proveedor preferido (solo uno activo por papel)';
COMMENT ON COLUMN indicolors.paper_supplier_prices.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.paper_supplier_prices.landed_cost_per_sheet IS 'sheet_value + freight_per_sheet (generada)';

GRANT ALL PRIVILEGES ON TABLE indicolors.paper_supplier_prices TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.paper_supplier_prices TO indicolors_app;

-- Datos semilla demo (dispara paper_price_history via trigger)
INSERT INTO indicolors.paper_supplier_prices (
    paper_supplier_price_id, company_id, paper_id, supplier_id,
    sheet_value, package_unit, freight_per_sheet, min_purchase_sheets, payment_days, delivery_days,
    preferred, state, price_date
) VALUES
    ('paper-price-seed-001', 'company-seed-001', 'paper-seed-001', 'supplier-seed-001', 1500.00, 500, 0.00, 500, 30, 5, TRUE, TRUE, CURRENT_DATE),
    ('paper-price-seed-002', 'company-seed-001', 'paper-seed-001', 'supplier-seed-002', 1480.00, 500, 20.00, 500, 15, 3, FALSE, TRUE, CURRENT_DATE),
    ('paper-price-seed-003', 'company-seed-001', 'paper-seed-002', 'supplier-seed-001', 3200.00, 250, 0.00, 250, 30, 7, TRUE, TRUE, CURRENT_DATE),
    ('paper-price-seed-004', 'company-seed-001', 'paper-seed-002', 'supplier-seed-002', 3100.00, 250, 50.00, 250, 45, 4, FALSE, TRUE, CURRENT_DATE),
    ('paper-price-seed-005', 'company-seed-001', 'paper-seed-003', 'supplier-seed-001', 2100.00, 300, 0.00, 300, 30, 5, TRUE, TRUE, CURRENT_DATE),
    ('paper-price-seed-006', 'company-seed-001', 'paper-seed-003', 'supplier-seed-003', 2050.00, 300, 30.00, 300, 0, 2, FALSE, TRUE, CURRENT_DATE)
ON CONFLICT (paper_supplier_price_id) DO NOTHING;
