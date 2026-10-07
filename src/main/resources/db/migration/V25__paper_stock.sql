CREATE TABLE indicolors.paper_stock (
    paper_stock_id     CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id         CHARACTER VARYING(64)       NOT NULL,
    paper_id           CHARACTER VARYING(64)       NOT NULL,
    quantity_initial   NUMERIC(12,2)               NOT NULL,
    quantity_available NUMERIC(12,2)               NOT NULL,
    unit_cost          NUMERIC(12,2)               NOT NULL,
    entry_date         DATE                        NOT NULL DEFAULT CURRENT_DATE,
    state              BOOLEAN                     NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT paper_stock_pkey PRIMARY KEY (paper_stock_id),
    CONSTRAINT paper_stock_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT paper_stock_paper_fk
        FOREIGN KEY (paper_id) REFERENCES indicolors.papers (paper_id),
    CONSTRAINT paper_stock_quantity_initial_check CHECK (quantity_initial >= 0),
    CONSTRAINT paper_stock_quantity_available_check
        CHECK (quantity_available >= 0 AND quantity_available <= quantity_initial),
    CONSTRAINT paper_stock_unit_cost_check CHECK (unit_cost >= 0)
);

CREATE INDEX idx_paper_stock_company_id ON indicolors.paper_stock (company_id);
CREATE INDEX idx_paper_stock_paper_id ON indicolors.paper_stock (paper_id);
CREATE INDEX idx_paper_stock_company_state ON indicolors.paper_stock (company_id, state);

COMMENT ON TABLE indicolors.paper_stock IS 'Inventario propio de papel (lotes). Sin triggers de consumo automático todavía';
COMMENT ON COLUMN indicolors.paper_stock.paper_stock_id IS 'Identificador único del lote';
COMMENT ON COLUMN indicolors.paper_stock.paper_id IS 'Papel inventariado (FK a papers)';
COMMENT ON COLUMN indicolors.paper_stock.quantity_initial IS 'Cantidad inicial del lote al ingresar';
COMMENT ON COLUMN indicolors.paper_stock.quantity_available IS 'Cantidad disponible restante';
COMMENT ON COLUMN indicolors.paper_stock.unit_cost IS 'Costo unitario del pliego en este lote';
COMMENT ON COLUMN indicolors.paper_stock.entry_date IS 'Fecha de ingreso del lote';
COMMENT ON COLUMN indicolors.paper_stock.state IS 'True=activo/disponible, False=inactivo';

GRANT ALL PRIVILEGES ON TABLE indicolors.paper_stock TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.paper_stock TO indicolors_app;

-- Datos semilla demo (lotes de inventario)
INSERT INTO indicolors.paper_stock (
    paper_stock_id, company_id, paper_id, quantity_initial, quantity_available, unit_cost, entry_date, state
) VALUES
    ('paper-stock-seed-001', 'company-seed-001', 'paper-seed-001', 2000.00, 1750.00, 1500.00, CURRENT_DATE - 30, TRUE),
    ('paper-stock-seed-002', 'company-seed-001', 'paper-seed-001', 500.00, 500.00, 1520.00, CURRENT_DATE - 7, TRUE),
    ('paper-stock-seed-003', 'company-seed-001', 'paper-seed-002', 800.00, 620.00, 3200.00, CURRENT_DATE - 14, TRUE),
    ('paper-stock-seed-004', 'company-seed-001', 'paper-seed-003', 1000.00, 1000.00, 2100.00, CURRENT_DATE, TRUE)
ON CONFLICT (paper_stock_id) DO NOTHING;
