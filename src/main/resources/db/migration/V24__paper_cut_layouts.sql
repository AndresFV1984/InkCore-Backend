CREATE TABLE indicolors.paper_cut_layouts (
    paper_cut_layout_id CHARACTER VARYING(64)  NOT NULL DEFAULT gen_random_uuid()::text,
    company_id          CHARACTER VARYING(64)  NOT NULL,
    paper_id            CHARACTER VARYING(64)  NOT NULL,
    cut_layout_id       CHARACTER VARYING(64)  NOT NULL,
    orientation         CHARACTER VARYING(16),
    waste_percentage    NUMERIC(5,2),
    note                TEXT,
    state               BOOLEAN                NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT paper_cut_layouts_pkey PRIMARY KEY (paper_cut_layout_id),
    CONSTRAINT paper_cut_layouts_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT paper_cut_layouts_paper_fk
        FOREIGN KEY (paper_id) REFERENCES indicolors.papers (paper_id) ON DELETE CASCADE,
    CONSTRAINT paper_cut_layouts_cut_layout_fk
        FOREIGN KEY (cut_layout_id) REFERENCES indicolors.cut_layouts (cut_layout_id),
    CONSTRAINT paper_cut_layouts_paper_cut_unique UNIQUE (paper_id, cut_layout_id),
    CONSTRAINT paper_cut_layouts_orientation_check
        CHECK (orientation IS NULL OR orientation IN ('vertical', 'horizontal')),
    CONSTRAINT paper_cut_layouts_waste_percentage_check
        CHECK (waste_percentage IS NULL OR (waste_percentage >= 0 AND waste_percentage <= 100))
);

CREATE INDEX idx_paper_cut_layouts_company_id ON indicolors.paper_cut_layouts (company_id);
CREATE INDEX idx_paper_cut_layouts_paper_id ON indicolors.paper_cut_layouts (paper_id);
CREATE INDEX idx_paper_cut_layouts_cut_layout_id ON indicolors.paper_cut_layouts (cut_layout_id);
CREATE INDEX idx_paper_cut_layouts_state ON indicolors.paper_cut_layouts (state);

COMMENT ON TABLE indicolors.paper_cut_layouts IS 'Relacion papel-despiece (cut_layouts). waste_percentage es sugerencia de UI; el calculo de OP usa company_waste_settings';
COMMENT ON COLUMN indicolors.paper_cut_layouts.paper_cut_layout_id IS 'Identificador único de la relación';
COMMENT ON COLUMN indicolors.paper_cut_layouts.paper_id IS 'Papel del catálogo (FK a papers)';
COMMENT ON COLUMN indicolors.paper_cut_layouts.cut_layout_id IS 'Despiece de catálogo (FK a cut_layouts)';
COMMENT ON COLUMN indicolors.paper_cut_layouts.orientation IS 'Orientación: vertical|horizontal';
COMMENT ON COLUMN indicolors.paper_cut_layouts.waste_percentage IS 'Sugerencia de desperdicio % para el formulario; no sustituye la merma de compañía en cálculo';
COMMENT ON COLUMN indicolors.paper_cut_layouts.note IS 'Observación libre';
COMMENT ON COLUMN indicolors.paper_cut_layouts.state IS 'True=Activo, False=Inactivo';

GRANT ALL PRIVILEGES ON TABLE indicolors.paper_cut_layouts TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.paper_cut_layouts TO indicolors_app;

-- Datos semilla demo (waste_percentage = sugerencia UI)
INSERT INTO indicolors.paper_cut_layouts (
    paper_cut_layout_id, company_id, paper_id, cut_layout_id, orientation, waste_percentage, note, state
) VALUES
    ('paper-cut-seed-001', 'company-seed-001', 'paper-seed-001', 'cut-layout-seed-001', 'vertical', 2.00, 'Sugerencia UI para etiquetas en Bond', TRUE),
    ('paper-cut-seed-002', 'company-seed-001', 'paper-seed-001', 'cut-layout-seed-002', 'horizontal', 2.00, NULL, TRUE),
    ('paper-cut-seed-003', 'company-seed-001', 'paper-seed-002', 'cut-layout-seed-001', 'vertical', 3.00, 'Sugerencia UI; merma OP usa company_waste_settings', TRUE),
    ('paper-cut-seed-004', 'company-seed-001', 'paper-seed-002', 'cut-layout-seed-002', 'horizontal', 3.00, NULL, TRUE),
    ('paper-cut-seed-005', 'company-seed-001', 'paper-seed-003', 'cut-layout-seed-001', 'vertical', 2.50, NULL, TRUE),
    ('paper-cut-seed-006', 'company-seed-001', 'paper-seed-003', 'cut-layout-seed-002', 'horizontal', 2.50, NULL, TRUE)
ON CONFLICT (paper_cut_layout_id) DO NOTHING;
