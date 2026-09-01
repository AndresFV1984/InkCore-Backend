-- Tabla paper_types (formulario "Nuevo tipo de papel").
-- Valor hoja y unidad empaque viven en paper_type_suppliers (no en esta tabla).

CREATE TABLE IF NOT EXISTS indicolors.paper_types (
    paper_type_id   VARCHAR(64)     NOT NULL DEFAULT gen_random_uuid()::text,
    company_id      VARCHAR(64)     NOT NULL,
    name            VARCHAR(150)    NOT NULL,
    width           NUMERIC(10,2)   NOT NULL,
    height          NUMERIC(10,2)   NOT NULL,
    unit            VARCHAR(10)     NOT NULL DEFAULT 'cm',
    is_coated       BOOLEAN         NOT NULL DEFAULT FALSE,
    state           BOOLEAN         NOT NULL DEFAULT TRUE,
    creation_date   DATE            NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT paper_types_pkey PRIMARY KEY (paper_type_id),
    CONSTRAINT paper_types_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT paper_types_name_company_unique UNIQUE (company_id, name),
    CONSTRAINT paper_types_width_check CHECK (width > 0),
    CONSTRAINT paper_types_height_check CHECK (height > 0),
    CONSTRAINT paper_types_unit_check CHECK (unit IN ('cm', 'mm', 'in'))
);

CREATE INDEX IF NOT EXISTS idx_paper_types_company_id ON indicolors.paper_types (company_id);
CREATE INDEX IF NOT EXISTS idx_paper_types_name ON indicolors.paper_types (name);
CREATE INDEX IF NOT EXISTS idx_paper_types_state ON indicolors.paper_types (state);
CREATE INDEX IF NOT EXISTS idx_paper_types_company_state ON indicolors.paper_types (company_id, state);
CREATE INDEX IF NOT EXISTS idx_paper_types_is_coated ON indicolors.paper_types (is_coated);

COMMENT ON TABLE indicolors.paper_types IS 'Catálogo de Tipos de papel, usados al configurar órdenes de producción';
COMMENT ON COLUMN indicolors.paper_types.paper_type_id IS 'Identificador único del tipo de papel';
COMMENT ON COLUMN indicolors.paper_types.company_id IS 'Identificador de la empresa dueña del tipo de papel';
COMMENT ON COLUMN indicolors.paper_types.name IS 'Nombre del tipo de papel';
COMMENT ON COLUMN indicolors.paper_types.width IS 'Ancho de la hoja/pliego';
COMMENT ON COLUMN indicolors.paper_types.height IS 'Alto de la hoja/pliego';
COMMENT ON COLUMN indicolors.paper_types.unit IS 'Unidad de medida del ancho/alto: cm, mm o in';
COMMENT ON COLUMN indicolors.paper_types.is_coated IS 'True=Papel esmaltado (tiene recubrimiento esmaltado)';
COMMENT ON COLUMN indicolors.paper_types.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.paper_types.creation_date IS 'Fecha de registro del tipo de papel en el sistema';
