-- Tablas paper_types y paper_type_cut_layouts (formulario "Nuevo tipo de papel").

CREATE TABLE IF NOT EXISTS indicolors.paper_types (
    paper_type_id   VARCHAR(64)     NOT NULL DEFAULT gen_random_uuid()::text,
    company_id      VARCHAR(64)     NOT NULL,
    name            VARCHAR(150)    NOT NULL,
    width           NUMERIC(10,2)   NOT NULL,
    height          NUMERIC(10,2)   NOT NULL,
    unit            VARCHAR(10)     NOT NULL DEFAULT 'cm',
    sheet_value     NUMERIC(12,2)   NOT NULL,
    package_unit    INTEGER         NOT NULL,
    is_coated       BOOLEAN         NOT NULL DEFAULT FALSE,
    state           BOOLEAN         NOT NULL DEFAULT TRUE,
    creation_date   DATE            NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT paper_types_pkey PRIMARY KEY (paper_type_id),
    CONSTRAINT paper_types_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT paper_types_name_company_unique UNIQUE (company_id, name),
    CONSTRAINT paper_types_width_check CHECK (width > 0),
    CONSTRAINT paper_types_height_check CHECK (height > 0),
    CONSTRAINT paper_types_unit_check CHECK (unit IN ('cm', 'mm', 'in')),
    CONSTRAINT paper_types_sheet_value_check CHECK (sheet_value >= 0),
    CONSTRAINT paper_types_package_unit_check CHECK (package_unit > 0)
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
COMMENT ON COLUMN indicolors.paper_types.sheet_value IS 'Valor de la hoja/pliego';
COMMENT ON COLUMN indicolors.paper_types.package_unit IS 'Cantidad de hojas por unidad de empaque';
COMMENT ON COLUMN indicolors.paper_types.is_coated IS 'True=Papel esmaltado (tiene recubrimiento esmaltado)';
COMMENT ON COLUMN indicolors.paper_types.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.paper_types.creation_date IS 'Fecha de registro del tipo de papel en el sistema';

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
