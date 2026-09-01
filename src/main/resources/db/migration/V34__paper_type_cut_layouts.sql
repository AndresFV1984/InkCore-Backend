-- Tabla paper_type_cut_layouts (relación N:M tipos de papel ↔ despieces).

CREATE TABLE IF NOT EXISTS indicolors.paper_type_cut_layouts (
    paper_type_id VARCHAR(64) NOT NULL,
    cut_layout_id VARCHAR(64) NOT NULL,
    cut_value     NUMERIC(12,2),
    assigned_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT paper_type_cut_layouts_pkey PRIMARY KEY (paper_type_id, cut_layout_id),
    CONSTRAINT paper_type_cut_layouts_paper_type_fk
        FOREIGN KEY (paper_type_id) REFERENCES indicolors.paper_types (paper_type_id) ON DELETE CASCADE,
    CONSTRAINT paper_type_cut_layouts_cut_layout_fk
        FOREIGN KEY (cut_layout_id) REFERENCES indicolors.cut_layouts (cut_layout_id) ON DELETE CASCADE,
    CONSTRAINT paper_type_cut_layouts_cut_value_check
        CHECK (cut_value IS NULL OR cut_value >= 0)
);

CREATE INDEX IF NOT EXISTS idx_paper_type_cut_layouts_cut_layout_id
    ON indicolors.paper_type_cut_layouts (cut_layout_id);

COMMENT ON TABLE indicolors.paper_type_cut_layouts IS 'Relación N:M entre tipos de papel y despieces por pliego, con el valor de corte asociado a cada combinación';
COMMENT ON COLUMN indicolors.paper_type_cut_layouts.paper_type_id IS 'Identificador del tipo de papel';
COMMENT ON COLUMN indicolors.paper_type_cut_layouts.cut_layout_id IS 'Identificador del despiece por pliego asociado';
COMMENT ON COLUMN indicolors.paper_type_cut_layouts.cut_value IS 'Valor de corte para este despiece en este tipo de papel específico';
