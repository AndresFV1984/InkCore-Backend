-- Tabla cut_layouts (formulario "Nuevo despiece") — un archivo por tabla, DDL completo.

CREATE TABLE IF NOT EXISTS indicolors.cut_layouts (
    cut_layout_id     VARCHAR(64)     NOT NULL DEFAULT gen_random_uuid()::text,
    company_id        VARCHAR(64)     NOT NULL,
    name              VARCHAR(150)    NOT NULL,
    width             NUMERIC(10,2)   NOT NULL,
    height            NUMERIC(10,2)   NOT NULL,
    unit              VARCHAR(10)     NOT NULL DEFAULT 'cm',
    pieces_per_sheet  INTEGER         NOT NULL,
    state             BOOLEAN         NOT NULL DEFAULT TRUE,
    creation_date     DATE            NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT cut_layouts_pkey PRIMARY KEY (cut_layout_id),
    CONSTRAINT cut_layouts_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT cut_layouts_name_company_unique UNIQUE (company_id, name),
    CONSTRAINT cut_layouts_width_check CHECK (width > 0),
    CONSTRAINT cut_layouts_height_check CHECK (height > 0),
    CONSTRAINT cut_layouts_pieces_per_sheet_check CHECK (pieces_per_sheet > 0),
    CONSTRAINT cut_layouts_unit_check CHECK (unit IN ('cm', 'mm', 'in'))
);

CREATE INDEX IF NOT EXISTS idx_cut_layouts_company_id ON indicolors.cut_layouts (company_id);
CREATE INDEX IF NOT EXISTS idx_cut_layouts_name ON indicolors.cut_layouts (name);
CREATE INDEX IF NOT EXISTS idx_cut_layouts_state ON indicolors.cut_layouts (state);
CREATE INDEX IF NOT EXISTS idx_cut_layouts_company_state ON indicolors.cut_layouts (company_id, state);

COMMENT ON TABLE indicolors.cut_layouts IS 'Catálogo de Despieces / diseños de corte (ej. Etiqueta 10x5 cm), usados para calcular piezas por pliego al configurar órdenes de producción';
COMMENT ON COLUMN indicolors.cut_layouts.cut_layout_id IS 'Identificador único del despiece';
COMMENT ON COLUMN indicolors.cut_layouts.company_id IS 'Identificador de la empresa dueña del despiece';
COMMENT ON COLUMN indicolors.cut_layouts.name IS 'Nombre del despiece (ej. Etiqueta); sin medidas, se muestran aparte';
COMMENT ON COLUMN indicolors.cut_layouts.width IS 'Ancho de la pieza';
COMMENT ON COLUMN indicolors.cut_layouts.height IS 'Alto de la pieza';
COMMENT ON COLUMN indicolors.cut_layouts.unit IS 'Unidad de medida del ancho/alto: cm, mm o in';
COMMENT ON COLUMN indicolors.cut_layouts.pieces_per_sheet IS 'Cantidad de piezas que caben en un pliego';
COMMENT ON COLUMN indicolors.cut_layouts.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.cut_layouts.creation_date IS 'Fecha de registro del despiece en el sistema';
