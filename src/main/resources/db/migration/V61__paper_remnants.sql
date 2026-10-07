-- Remanentes reutilizables de corte: mismo papel (material) con medidas propias.
CREATE TABLE indicolors.paper_remnants (
    paper_remnant_id             CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                   CHARACTER VARYING(64)       NOT NULL,
    paper_id                     CHARACTER VARYING(64)       NOT NULL,
    width                        NUMERIC(10,2)               NOT NULL,
    height                       NUMERIC(10,2)               NOT NULL,
    unit                         CHARACTER VARYING(10)       NOT NULL DEFAULT 'cm',
    quantity_initial             NUMERIC(12,2)               NOT NULL,
    quantity_available           NUMERIC(12,2)               NOT NULL,
    unit_cost                    NUMERIC(12,2)               NOT NULL DEFAULT 0,
    source_production_order_id   CHARACTER VARYING(64),
    source_paper_row_id          CHARACTER VARYING(64),
    entry_date                   DATE                        NOT NULL DEFAULT CURRENT_DATE,
    note                         CHARACTER VARYING(500),
    state                        BOOLEAN                     NOT NULL DEFAULT TRUE,
    created_at                   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT paper_remnants_pkey PRIMARY KEY (paper_remnant_id),
    CONSTRAINT paper_remnants_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT paper_remnants_paper_fk
        FOREIGN KEY (paper_id) REFERENCES indicolors.papers (paper_id),
    CONSTRAINT paper_remnants_source_order_fk
        FOREIGN KEY (source_production_order_id)
            REFERENCES indicolors.production_orders (production_order_id) ON DELETE SET NULL,
    CONSTRAINT paper_remnants_source_paper_row_fk
        FOREIGN KEY (source_paper_row_id)
            REFERENCES indicolors.production_order_paper_rows (production_order_paper_row_id)
            ON DELETE SET NULL,
    CONSTRAINT paper_remnants_width_check CHECK (width > 0),
    CONSTRAINT paper_remnants_height_check CHECK (height > 0),
    CONSTRAINT paper_remnants_unit_check CHECK (unit IN ('cm', 'mm', 'in')),
    CONSTRAINT paper_remnants_quantity_initial_check CHECK (quantity_initial >= 0),
    CONSTRAINT paper_remnants_quantity_available_check
        CHECK (quantity_available >= 0 AND quantity_available <= quantity_initial),
    CONSTRAINT paper_remnants_unit_cost_check CHECK (unit_cost >= 0)
);

CREATE INDEX idx_paper_remnants_company_id ON indicolors.paper_remnants (company_id);
CREATE INDEX idx_paper_remnants_paper_id ON indicolors.paper_remnants (paper_id);
CREATE INDEX idx_paper_remnants_company_state ON indicolors.paper_remnants (company_id, state);
CREATE INDEX idx_paper_remnants_source_order_id ON indicolors.paper_remnants (source_production_order_id);
CREATE INDEX idx_paper_remnants_source_paper_row_id ON indicolors.paper_remnants (source_paper_row_id);

COMMENT ON TABLE indicolors.paper_remnants IS
    'Remanentes reutilizables de corte: material del papel origen con medidas distintas al formato de compra';
COMMENT ON COLUMN indicolors.paper_remnants.paper_remnant_id IS 'Identificador único del remanente';
COMMENT ON COLUMN indicolors.paper_remnants.paper_id IS 'Papel de origen (identidad de material; FK a papers)';
COMMENT ON COLUMN indicolors.paper_remnants.width IS 'Ancho del remanente (no del pliego de compra)';
COMMENT ON COLUMN indicolors.paper_remnants.height IS 'Alto del remanente (no del pliego de compra)';
COMMENT ON COLUMN indicolors.paper_remnants.unit IS 'Unidad de medida del remanente: cm, mm o in';
COMMENT ON COLUMN indicolors.paper_remnants.quantity_initial IS 'Cantidad inicial de piezas/pliegos remanentes';
COMMENT ON COLUMN indicolors.paper_remnants.quantity_available IS 'Cantidad disponible restante';
COMMENT ON COLUMN indicolors.paper_remnants.unit_cost IS 'Costo unitario del remanente (0 si no se valoriza)';
COMMENT ON COLUMN indicolors.paper_remnants.source_production_order_id IS 'OP que generó el remanente (opcional)';
COMMENT ON COLUMN indicolors.paper_remnants.source_paper_row_id IS 'Fila de corte origen (opcional)';
COMMENT ON COLUMN indicolors.paper_remnants.entry_date IS 'Fecha de ingreso del remanente';
COMMENT ON COLUMN indicolors.paper_remnants.note IS 'Nota libre';
COMMENT ON COLUMN indicolors.paper_remnants.state IS 'True=disponible para uso, False=inactivo';

GRANT ALL PRIVILEGES ON TABLE indicolors.paper_remnants TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.paper_remnants TO indicolors_app;

-- Datos semilla demo
INSERT INTO indicolors.paper_remnants (
    paper_remnant_id, company_id, paper_id, width, height, unit,
    quantity_initial, quantity_available, unit_cost, entry_date, note, state
) VALUES
    ('paper-remnant-seed-001', 'company-seed-001', 'paper-seed-001',
     35.00, 50.00, 'cm', 12.00, 12.00, 0.00, CURRENT_DATE - 5,
     'Sobrante de corte Bond 70x100', TRUE),
    ('paper-remnant-seed-002', 'company-seed-001', 'paper-seed-002',
     36.00, 51.00, 'cm', 4.00, 3.00, 800.00, CURRENT_DATE - 2,
     'Remanente Couche reutilizable', TRUE)
ON CONFLICT (paper_remnant_id) DO NOTHING;
