-- Tabla production_order_print (módulo production-orders).

CREATE TABLE IF NOT EXISTS indicolors.production_order_print (
    production_order_print_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                CHARACTER VARYING(64)       NOT NULL,
    production_order_id       CHARACTER VARYING(64)       NOT NULL,
    plate_id                  CHARACTER VARYING(64)       NOT NULL,

    client_supplies_sherpa    BOOLEAN,
    sherpa_test_price         NUMERIC(12,2),
    machine_output_value      NUMERIC(12,2),
    ink_estimation            JSONB,

    printing_discount_type    CHARACTER VARYING(10),
    printing_discount_value   NUMERIC(12,2),
    completed                 BOOLEAN                     NOT NULL DEFAULT FALSE,

    created_at                TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_print_pkey PRIMARY KEY (production_order_print_id),
    CONSTRAINT production_order_print_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_print_order_fk FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_print_plate_fk FOREIGN KEY (plate_id) REFERENCES indicolors.production_order_plates (production_order_plate_id),
    CONSTRAINT production_order_print_plate_unique UNIQUE (plate_id)
);

CREATE INDEX IF NOT EXISTS idx_production_order_print_company_id ON indicolors.production_order_print (company_id);
CREATE INDEX IF NOT EXISTS idx_production_order_print_order_id ON indicolors.production_order_print (production_order_id);
CREATE INDEX IF NOT EXISTS idx_prints_ink_estimation_gin
    ON indicolors.production_order_print
    USING gin (ink_estimation jsonb_path_ops);

COMMENT ON TABLE indicolors.production_order_print IS 'Configuración de impresión por plancha; máximo 1 registro por plancha (UNIQUE plate_id)';

COMMENT ON COLUMN indicolors.production_order_print.production_order_print_id IS 'Identificador único de la configuración de impresión';
COMMENT ON COLUMN indicolors.production_order_print.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_print.production_order_id IS 'Identificador de la Orden de Producción a la que pertenece';
COMMENT ON COLUMN indicolors.production_order_print.plate_id IS 'Identificador de la plancha a la que pertenece (FK, único por plancha)';
COMMENT ON COLUMN indicolors.production_order_print.client_supplies_sherpa IS 'True=el cliente suministra la prueba Sherpa';
COMMENT ON COLUMN indicolors.production_order_print.sherpa_test_price IS 'Precio de la prueba Sherpa, cuando client_supplies_sherpa=false';
COMMENT ON COLUMN indicolors.production_order_print.machine_output_value IS 'Valor de salida de máquina';
COMMENT ON COLUMN indicolors.production_order_print.ink_estimation IS 'JSONB: metadatos numéricos de estimación de tintas + objectKey/previewObjectKey (sin Base64/data-URL)';
COMMENT ON COLUMN indicolors.production_order_print.printing_discount_type IS 'Tipo de descuento de Impresión para esta plancha: % | $';
COMMENT ON COLUMN indicolors.production_order_print.printing_discount_value IS 'Valor del descuento de Impresión para esta plancha';
COMMENT ON COLUMN indicolors.production_order_print.completed IS 'True=el paso de Impresión para esta plancha está finalizado';
COMMENT ON COLUMN indicolors.production_order_print.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_print.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_print TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_print TO indicolors_app;
