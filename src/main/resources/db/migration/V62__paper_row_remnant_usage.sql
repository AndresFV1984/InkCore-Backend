-- Origen remanente en filas de corte + cantidad descontada del stock de remanentes.
ALTER TABLE indicolors.production_order_paper_rows
    ADD COLUMN IF NOT EXISTS paper_remnant_id CHARACTER VARYING(64),
    ADD COLUMN IF NOT EXISTS remnant_quantity_used NUMERIC(12,2);

ALTER TABLE indicolors.production_order_paper_rows
    DROP CONSTRAINT IF EXISTS production_order_paper_rows_paper_remnant_fk;

ALTER TABLE indicolors.production_order_paper_rows
    ADD CONSTRAINT production_order_paper_rows_paper_remnant_fk
        FOREIGN KEY (paper_remnant_id)
            REFERENCES indicolors.paper_remnants (paper_remnant_id)
            ON DELETE SET NULL;

ALTER TABLE indicolors.production_order_paper_rows
    DROP CONSTRAINT IF EXISTS production_order_paper_rows_remnant_qty_check;

ALTER TABLE indicolors.production_order_paper_rows
    ADD CONSTRAINT production_order_paper_rows_remnant_qty_check
        CHECK (remnant_quantity_used IS NULL OR remnant_quantity_used >= 0);

CREATE INDEX IF NOT EXISTS idx_production_order_paper_rows_paper_remnant_id
    ON indicolors.production_order_paper_rows (paper_remnant_id);

COMMENT ON COLUMN indicolors.production_order_paper_rows.paper_remnant_id IS
    'Remanente usado como origen del corte (opcional; FK paper_remnants)';
COMMENT ON COLUMN indicolors.production_order_paper_rows.remnant_quantity_used IS
    'Unidades descontadas de quantity_available del remanente al guardar el corte';
