CREATE TABLE indicolors.papers (
    paper_id             CHARACTER VARYING(64)  NOT NULL DEFAULT gen_random_uuid()::text,
    company_id           CHARACTER VARYING(64)  NOT NULL,
    name                 CHARACTER VARYING(150) NOT NULL,
    grammage             NUMERIC(6,2),
    width                NUMERIC(10,2)          NOT NULL,
    height               NUMERIC(10,2)          NOT NULL,
    unit                 CHARACTER VARYING(10)  NOT NULL DEFAULT 'cm',
    is_coated            BOOLEAN                NOT NULL DEFAULT FALSE,
    accepts_remnants     BOOLEAN                NOT NULL DEFAULT FALSE,
    min_remnant_width    NUMERIC(10,2),
    min_remnant_height   NUMERIC(10,2),
    min_remnant_unit     CHARACTER VARYING(10),
    state                BOOLEAN                NOT NULL DEFAULT TRUE,
    creation_date        DATE                   NOT NULL DEFAULT CURRENT_DATE,
    updated_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT papers_pkey PRIMARY KEY (paper_id),
    CONSTRAINT papers_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT papers_grammage_check
        CHECK (grammage IS NULL OR grammage > 0),
    CONSTRAINT papers_width_check CHECK (width > 0),
    CONSTRAINT papers_height_check CHECK (height > 0),
    CONSTRAINT papers_unit_check CHECK (unit IN ('cm', 'mm', 'in')),
    CONSTRAINT papers_min_remnant_unit_check
        CHECK (min_remnant_unit IS NULL OR min_remnant_unit IN ('cm', 'mm', 'in')),
    CONSTRAINT papers_remnant_policy_check CHECK (
        (accepts_remnants = FALSE
            AND min_remnant_width IS NULL
            AND min_remnant_height IS NULL
            AND min_remnant_unit IS NULL)
        OR
        (accepts_remnants = TRUE
            AND min_remnant_width IS NOT NULL
            AND min_remnant_height IS NOT NULL
            AND min_remnant_unit IS NOT NULL
            AND min_remnant_width > 0
            AND min_remnant_height > 0)
    ),
    CONSTRAINT papers_company_name_grammage_format_unique
        UNIQUE NULLS NOT DISTINCT (company_id, name, grammage, width, height, unit)
);

CREATE INDEX idx_papers_company_id ON indicolors.papers (company_id);
CREATE INDEX idx_papers_name ON indicolors.papers (name);
CREATE INDEX idx_papers_state ON indicolors.papers (state);
CREATE INDEX idx_papers_company_state ON indicolors.papers (company_id, state);
CREATE INDEX idx_papers_is_coated ON indicolors.papers (is_coated);
CREATE INDEX idx_papers_company_accepts_remnants ON indicolors.papers (company_id, accepts_remnants);

COMMENT ON TABLE indicolors.papers IS 'Catalogo de papeles por compania: tipo, gramaje, formato (width x height x unit), esmaltado, politica de remanentes y estado';
COMMENT ON COLUMN indicolors.papers.paper_id IS 'Identificador unico del papel';
COMMENT ON COLUMN indicolors.papers.company_id IS 'Identificador de la empresa duena del papel';
COMMENT ON COLUMN indicolors.papers.name IS 'Tipo o nombre comercial del papel (ej. Bond, Couche)';
COMMENT ON COLUMN indicolors.papers.grammage IS 'Gramaje en g por m2; NULL si aun no se conoce';
COMMENT ON COLUMN indicolors.papers.width IS 'Ancho del formato o pliego de compra';
COMMENT ON COLUMN indicolors.papers.height IS 'Alto del formato o pliego de compra';
COMMENT ON COLUMN indicolors.papers.unit IS 'Unidad de medida del pliego de compra: cm, mm o in';
COMMENT ON COLUMN indicolors.papers.is_coated IS 'True=papel esmaltado o estucado';
COMMENT ON COLUMN indicolors.papers.accepts_remnants IS 'True=papel candidable a remanentes (guia UI; el API de remanentes no bloquea)';
COMMENT ON COLUMN indicolors.papers.min_remnant_width IS 'Ancho minimo sugerido de remanente; obligatorio si accepts_remnants';
COMMENT ON COLUMN indicolors.papers.min_remnant_height IS 'Alto minimo sugerido de remanente; obligatorio si accepts_remnants';
COMMENT ON COLUMN indicolors.papers.min_remnant_unit IS 'Unidad de las medidas minimas de remanente: cm, mm o in; obligatoria si accepts_remnants';
COMMENT ON COLUMN indicolors.papers.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.papers.creation_date IS 'Fecha de registro del papel en el sistema';
COMMENT ON COLUMN indicolors.papers.updated_at IS 'Fecha y hora de la ultima actualizacion del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.papers TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.papers TO indicolors_app;

-- Datos semilla demo (company-seed-001)
INSERT INTO indicolors.papers (
    paper_id, company_id, name, grammage, width, height, unit, is_coated,
    accepts_remnants, min_remnant_width, min_remnant_height, min_remnant_unit, state, creation_date
) VALUES
    ('paper-seed-001', 'company-seed-001', 'Bond 75g', 75.00, 70.00, 100.00, 'cm', FALSE,
     TRUE, 20.00, 20.00, 'cm', TRUE, CURRENT_DATE),
    ('paper-seed-002', 'company-seed-001', 'Couche 150g', 150.00, 72.00, 102.00, 'cm', TRUE,
     TRUE, 25.00, 25.00, 'cm', TRUE, CURRENT_DATE),
    ('paper-seed-003', 'company-seed-001', 'Propalcote 115g', 115.00, 70.00, 100.00, 'cm', TRUE,
     FALSE, NULL, NULL, NULL, TRUE, CURRENT_DATE)
ON CONFLICT (paper_id) DO NOTHING;
