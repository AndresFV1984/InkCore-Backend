-- Tabla production_order_paper_rows (módulo production-orders).
-- DDL completo: todas las columnas en el CREATE (sin migraciones ADD COLUMN).

CREATE TABLE IF NOT EXISTS indicolors.production_order_paper_rows (
    production_order_paper_row_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                    CHARACTER VARYING(64)       NOT NULL,
    production_order_id           CHARACTER VARYING(64)       NOT NULL,
    plate_id                      CHARACTER VARYING(64)       NOT NULL,
    parent_row_id                 CHARACTER VARYING(64),  -- self-FK (fila de faltante)

    cut_row_key                   CHARACTER VARYING(50)       NOT NULL,
    is_missing_supply             BOOLEAN                     NOT NULL DEFAULT FALSE,
    missing_sheets_quantity       INTEGER,

    client_supplies_paper         BOOLEAN                     NOT NULL,

    paper_type_id                 CHARACTER VARYING(64),
    supplier_id                   CHARACTER VARYING(64),
    paper_name                    CHARACTER VARYING(80),
    paper_size                    CHARACTER VARYING(30),
    sheet_value                   NUMERIC(12,2),
    package_unit                  INTEGER,
    is_coated                     BOOLEAN,

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

    created_at                    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_paper_rows_pkey PRIMARY KEY (production_order_paper_row_id),
    CONSTRAINT production_order_paper_rows_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_paper_rows_order_fk FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_paper_rows_plate_fk FOREIGN KEY (plate_id) REFERENCES indicolors.production_order_plates (production_order_plate_id),
    CONSTRAINT production_order_paper_rows_parent_row_fk FOREIGN KEY (parent_row_id) REFERENCES indicolors.production_order_paper_rows (production_order_paper_row_id),
    CONSTRAINT production_order_paper_rows_paper_type_fk FOREIGN KEY (paper_type_id) REFERENCES indicolors.paper_types (paper_type_id),
    CONSTRAINT production_order_paper_rows_supplier_fk FOREIGN KEY (supplier_id) REFERENCES indicolors.suppliers (supplier_id) ON DELETE SET NULL,
    CONSTRAINT production_order_paper_rows_cut_layout_fk FOREIGN KEY (cut_layout_id) REFERENCES indicolors.cut_layouts (cut_layout_id)
);

CREATE INDEX IF NOT EXISTS idx_production_order_paper_rows_company_id ON indicolors.production_order_paper_rows (company_id);
CREATE INDEX IF NOT EXISTS idx_production_order_paper_rows_order_id ON indicolors.production_order_paper_rows (production_order_id);
CREATE INDEX IF NOT EXISTS idx_production_order_paper_rows_plate_id ON indicolors.production_order_paper_rows (plate_id);
CREATE INDEX IF NOT EXISTS idx_production_order_paper_rows_parent_row_id ON indicolors.production_order_paper_rows (parent_row_id);
CREATE INDEX IF NOT EXISTS idx_production_order_paper_rows_cut_row_key ON indicolors.production_order_paper_rows (cut_row_key);
CREATE INDEX IF NOT EXISTS idx_production_order_paper_rows_paper_type_id ON indicolors.production_order_paper_rows (paper_type_id);
CREATE INDEX IF NOT EXISTS idx_production_order_paper_rows_supplier_id ON indicolors.production_order_paper_rows (supplier_id);
CREATE INDEX IF NOT EXISTS idx_production_order_paper_rows_cut_layout_id ON indicolors.production_order_paper_rows (cut_layout_id);

COMMENT ON TABLE indicolors.production_order_paper_rows IS 'Corte de papel por plancha, 0..N filas (incluye filas de faltante cubierto por litografía)';

COMMENT ON COLUMN indicolors.production_order_paper_rows.production_order_paper_row_id IS 'Identificador único de la fila de corte de papel';
COMMENT ON COLUMN indicolors.production_order_paper_rows.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_paper_rows.production_order_id IS 'Identificador de la Orden de Producción a la que pertenece la fila';
COMMENT ON COLUMN indicolors.production_order_paper_rows.plate_id IS 'Identificador de la plancha a la que pertenece la fila (FK a production_order_plates)';
COMMENT ON COLUMN indicolors.production_order_paper_rows.parent_row_id IS 'Self-FK: fila de corte original cuando esta fila es un faltante cubierto por litografía';
COMMENT ON COLUMN indicolors.production_order_paper_rows.cut_row_key IS 'Clave de agrupación/visualización del frontend para identificar filas de corte de una misma plancha (incluye faltantes). NO es FK ni se referencia desde production_order_postpress_records: Terminados y Acabados se vinculan a nivel de plancha vía plate_id, no por fila de corte individual';
COMMENT ON COLUMN indicolors.production_order_paper_rows.is_missing_supply IS 'True=esta fila representa un faltante de papel cubierto por litografía';
COMMENT ON COLUMN indicolors.production_order_paper_rows.missing_sheets_quantity IS 'Cantidad de pliegos faltantes cubiertos, cuando is_missing_supply=true';
COMMENT ON COLUMN indicolors.production_order_paper_rows.client_supplies_paper IS 'True=el cliente suministra el papel de esta fila';
COMMENT ON COLUMN indicolors.production_order_paper_rows.paper_type_id IS 'Identificador del tipo de papel seleccionado (FK a paper_types)';
COMMENT ON COLUMN indicolors.production_order_paper_rows.supplier_id IS 'Proveedor del tipo de papel usado para valor hoja y unidad empaque en este corte';
COMMENT ON COLUMN indicolors.production_order_paper_rows.paper_name IS 'Snapshot del nombre del tipo de papel al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_paper_rows.paper_size IS 'Snapshot de la medida del tipo de papel al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_paper_rows.sheet_value IS 'Snapshot del valor del pliego al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_paper_rows.package_unit IS 'Snapshot de la unidad de empaque del tipo de papel';
COMMENT ON COLUMN indicolors.production_order_paper_rows.is_coated IS 'Snapshot de si el papel es estucado/recubierto';
COMMENT ON COLUMN indicolors.production_order_paper_rows.cut_layout_id IS 'Identificador del patrón de corte seleccionado (FK a cut_layouts)';
COMMENT ON COLUMN indicolors.production_order_paper_rows.cut_layout_name IS 'Snapshot del nombre del patrón de corte al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_paper_rows.cut_layout_size IS 'Snapshot de la medida del patrón de corte al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_paper_rows.pieces_per_sheet IS 'Snapshot de piezas por pliego del patrón de corte';
COMMENT ON COLUMN indicolors.production_order_paper_rows.cut_value IS 'Snapshot del valor de corte al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_paper_rows.is_paper_cut IS 'Cuando el cliente suministra el papel: True=el papel ya viene cortado';
COMMENT ON COLUMN indicolors.production_order_paper_rows.delivered_sheets_by_client IS 'Cantidad de pliegos entregados por el cliente, cuando is_paper_cut=true';
COMMENT ON COLUMN indicolors.production_order_paper_rows.manual_good_sizes IS 'Cantidad de piezas buenas ingresada manualmente, cuando is_paper_cut=false';
COMMENT ON COLUMN indicolors.production_order_paper_rows.manual_surplus IS 'Excedente ingresado manualmente, cuando is_paper_cut=false';
COMMENT ON COLUMN indicolors.production_order_paper_rows.calculated_sheets_count IS 'Cantidad de pliegos calculada en servidor';
COMMENT ON COLUMN indicolors.production_order_paper_rows.total_paper_value IS 'Valor total de papel, calculado en servidor';
COMMENT ON COLUMN indicolors.production_order_paper_rows.total_cut_value IS 'Valor total de corte, calculado en servidor';
COMMENT ON COLUMN indicolors.production_order_paper_rows.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_paper_rows.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_paper_rows TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_paper_rows TO indicolors_app;
