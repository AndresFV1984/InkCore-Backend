-- Tabla production_order_paper_rows (módulo production-orders).
-- DDL completo: todas las columnas en el CREATE (sin migraciones ADD COLUMN).

CREATE TABLE indicolors.production_order_paper_rows (
    production_order_paper_row_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                    CHARACTER VARYING(64)       NOT NULL,
    production_order_id           CHARACTER VARYING(64)       NOT NULL,
    plate_id                      CHARACTER VARYING(64)       NOT NULL,
    parent_row_id                 CHARACTER VARYING(64),

    cut_row_key                   CHARACTER VARYING(50)       NOT NULL,
    is_missing_supply             BOOLEAN                     NOT NULL DEFAULT FALSE,
    missing_sheets_quantity       INTEGER,

    client_supplies_paper         BOOLEAN                     NOT NULL,

    paper_id                      CHARACTER VARYING(64),
    supplier_id                   CHARACTER VARYING(64),
    paper_name                    CHARACTER VARYING(80),
    paper_size                    CHARACTER VARYING(30),
    sheet_value                   NUMERIC(12,2),
    package_unit                  INTEGER,
    is_coated                     BOOLEAN,
    freight_per_sheet_snapshot    NUMERIC(12,2),
    price_date_snapshot           DATE,
    price_rule                    CHARACTER VARYING(20),
    pieces_per_sheet_snapshot     INTEGER,
    net_sheets                    NUMERIC(12,2),
    waste_sheets                  NUMERIC(12,2),
    total_sheets                  NUMERIC(12,2),
    cost_per_piece                NUMERIC(12,2),
    is_coated_snapshot            BOOLEAN,

    cut_layout_id                 CHARACTER VARYING(64),
    cut_layout_name               CHARACTER VARYING(80),
    cut_layout_size               CHARACTER VARYING(30),
    pieces_per_sheet              SMALLINT,
    cut_value                     NUMERIC(12,2),

    is_paper_cut                  BOOLEAN,
    delivered_sheets_by_client    INTEGER,
    manual_good_sizes             INTEGER,
    manual_surplus                INTEGER,

    calculated_sheets_count       INTEGER,
    total_paper_value             NUMERIC(12,2),
    total_cut_value               NUMERIC(12,2),
    planned_waste_percentage      NUMERIC(5,2)                NOT NULL DEFAULT 0,

    created_at                    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_paper_rows_pkey PRIMARY KEY (production_order_paper_row_id),
    CONSTRAINT production_order_paper_rows_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_paper_rows_order_fk FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_paper_rows_plate_fk FOREIGN KEY (plate_id) REFERENCES indicolors.production_order_plates (production_order_plate_id),
    CONSTRAINT production_order_paper_rows_parent_row_fk FOREIGN KEY (parent_row_id) REFERENCES indicolors.production_order_paper_rows (production_order_paper_row_id),
    CONSTRAINT production_order_paper_rows_paper_fk FOREIGN KEY (paper_id) REFERENCES indicolors.papers (paper_id),
    CONSTRAINT production_order_paper_rows_supplier_fk FOREIGN KEY (supplier_id) REFERENCES indicolors.suppliers (supplier_id) ON DELETE SET NULL,
    CONSTRAINT production_order_paper_rows_cut_layout_fk FOREIGN KEY (cut_layout_id) REFERENCES indicolors.cut_layouts (cut_layout_id),
    CONSTRAINT production_order_paper_rows_planned_waste_percentage_check
        CHECK (planned_waste_percentage >= 0 AND planned_waste_percentage <= 100),
    CONSTRAINT production_order_paper_rows_price_rule_check
        CHECK (price_rule IS NULL OR price_rule IN ('PREFERRED', 'REPLACEMENT', 'BEST_COST'))
);

CREATE INDEX idx_production_order_paper_rows_company_id ON indicolors.production_order_paper_rows (company_id);
CREATE INDEX idx_production_order_paper_rows_order_id ON indicolors.production_order_paper_rows (production_order_id);
CREATE INDEX idx_production_order_paper_rows_plate_id ON indicolors.production_order_paper_rows (plate_id);
CREATE INDEX idx_production_order_paper_rows_parent_row_id ON indicolors.production_order_paper_rows (parent_row_id);
CREATE INDEX idx_production_order_paper_rows_cut_row_key ON indicolors.production_order_paper_rows (cut_row_key);
CREATE INDEX idx_production_order_paper_rows_paper_id ON indicolors.production_order_paper_rows (paper_id);
CREATE INDEX idx_production_order_paper_rows_supplier_id ON indicolors.production_order_paper_rows (supplier_id);
CREATE INDEX idx_production_order_paper_rows_cut_layout_id ON indicolors.production_order_paper_rows (cut_layout_id);

COMMENT ON TABLE indicolors.production_order_paper_rows IS 'Corte de papel por plancha, 0..N filas (incluye filas de faltante cubierto por litografía)';
COMMENT ON COLUMN indicolors.production_order_paper_rows.paper_id IS 'Papel seleccionado del catálogo (FK a papers)';
COMMENT ON COLUMN indicolors.production_order_paper_rows.supplier_id IS 'Proveedor del precio usado para valor hoja en este corte';
COMMENT ON COLUMN indicolors.production_order_paper_rows.paper_name IS 'Snapshot del nombre del papel al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_paper_rows.paper_size IS 'Snapshot del formato del papel al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_paper_rows.planned_waste_percentage IS 'Merma de corte aplicada (company_waste_settings u override de OP; no del waste sugerido del papel-despiece)';
COMMENT ON COLUMN indicolors.production_order_paper_rows.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_paper_rows.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_paper_rows TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_paper_rows TO indicolors_app;
