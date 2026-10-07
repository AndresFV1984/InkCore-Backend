-- Tabla production_order_postpress_lines (módulo production-orders).

CREATE TABLE IF NOT EXISTS indicolors.production_order_postpress_lines (
    production_order_postpress_line_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                         CHARACTER VARYING(64)       NOT NULL,
    record_id                          CHARACTER VARYING(64)       NOT NULL,

    catalog_item_id                    CHARACTER VARYING(64),  -- FK lógica a finished_products o finishing_processes según el type del record padre
    item_name                          CHARACTER VARYING(150),
    source                             CHARACTER VARYING(15)       NOT NULL,  -- catalog | quick-access

    value_per_cm2                      NUMERIC(12,4),
    min_cost                           NUMERIC(12,2),
    area_factor                        NUMERIC(10,4),
    good_sizes                         INTEGER,
    calculated_price                   NUMERIC(12,2),
    charged_price                      NUMERIC(12,2),
    applied_min_cost                   BOOLEAN                     NOT NULL DEFAULT FALSE,

    positive                           BOOLEAN,  -- solo type=FINISHED_PRODUCT (Reserva UV)
    cliche                             BOOLEAN,  -- solo type=FINISHED_PRODUCT (Estampado)

    created_at                         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_postpress_lines_pkey PRIMARY KEY (production_order_postpress_line_id),
    CONSTRAINT production_order_postpress_lines_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_postpress_lines_record_fk FOREIGN KEY (record_id) REFERENCES indicolors.production_order_postpress_records (production_order_postpress_record_id) ON DELETE CASCADE,
    CONSTRAINT production_order_postpress_lines_source_check CHECK (source IN ('catalog','quick-access'))
);

CREATE INDEX IF NOT EXISTS idx_production_order_postpress_lines_company_id ON indicolors.production_order_postpress_lines (company_id);
CREATE INDEX IF NOT EXISTS idx_production_order_postpress_lines_record_id ON indicolors.production_order_postpress_lines (record_id);
CREATE INDEX IF NOT EXISTS idx_production_order_postpress_lines_catalog_item_id ON indicolors.production_order_postpress_lines (catalog_item_id);

COMMENT ON TABLE indicolors.production_order_postpress_lines IS 'Líneas de costeo confirmadas dentro de un registro de Terminados/Acabados, 0..N por registro';

COMMENT ON COLUMN indicolors.production_order_postpress_lines.production_order_postpress_line_id IS 'Identificador único de la línea de costeo';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.record_id IS 'Identificador del registro de Terminados/Acabados al que pertenece la línea (FK a production_order_postpress_records)';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.catalog_item_id IS 'Referencia lógica a finished_products.finished_product_id o finishing_processes.finishing_process_id, según el type del registro padre; validar en el Service, no hay FK física por ser polimórfico';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.item_name IS 'Snapshot del nombre del ítem de catálogo al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.source IS 'Origen de selección del ítem: catalog (select completo) | quick-access (acceso rápido)';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.value_per_cm2 IS 'Snapshot del valor por cm² del ítem de catálogo';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.min_cost IS 'Snapshot del costo mínimo del ítem de catálogo';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.area_factor IS 'Factor de área usado en el cálculo del precio';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.good_sizes IS 'Cantidad de piezas buenas sobre las que se calcula el costeo';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.calculated_price IS 'Precio calculado en servidor antes de aplicar el costo mínimo';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.charged_price IS 'Precio finalmente cobrado (igual a calculated_price o a min_cost si aplica)';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.applied_min_cost IS 'True si se cobró el costo mínimo del catálogo en vez del precio calculado';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.positive IS 'Solo aplica cuando el registro padre es type=FINISHED_PRODUCT y el ítem es Reserva UV';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.cliche IS 'Solo aplica cuando el registro padre es type=FINISHED_PRODUCT y el ítem es Estampado';
COMMENT ON COLUMN indicolors.production_order_postpress_lines.created_at IS 'Fecha y hora de creación del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_postpress_lines TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_postpress_lines TO indicolors_app;
