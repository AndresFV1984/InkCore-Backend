-- Tabla production_order_postpress_records (módulo production-orders).

CREATE TABLE IF NOT EXISTS indicolors.production_order_postpress_records (
    production_order_postpress_record_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                           CHARACTER VARYING(64)       NOT NULL,
    production_order_id                  CHARACTER VARYING(64)       NOT NULL,
    plate_id                             CHARACTER VARYING(64)       NOT NULL,

    type                                 CHARACTER VARYING(20)       NOT NULL, -- FINISHED_PRODUCT | FINISHING_PROCESS
    completed                            BOOLEAN                     NOT NULL DEFAULT FALSE,

    created_at                           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_postpress_records_pkey PRIMARY KEY (production_order_postpress_record_id),
    CONSTRAINT production_order_postpress_records_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_postpress_records_order_fk FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_postpress_records_plate_fk FOREIGN KEY (plate_id) REFERENCES indicolors.production_order_plates (production_order_plate_id),
    CONSTRAINT production_order_postpress_records_type_check CHECK (type IN ('FINISHED_PRODUCT','FINISHING_PROCESS')),
    CONSTRAINT production_order_postpress_records_plate_type_unique UNIQUE (plate_id, type)
);

CREATE INDEX IF NOT EXISTS idx_production_order_postpress_records_company_id ON indicolors.production_order_postpress_records (company_id);
CREATE INDEX IF NOT EXISTS idx_production_order_postpress_records_order_id ON indicolors.production_order_postpress_records (production_order_id);
CREATE INDEX IF NOT EXISTS idx_production_order_postpress_records_type ON indicolors.production_order_postpress_records (type);

COMMENT ON TABLE indicolors.production_order_postpress_records IS 'Registro de Terminados o Acabados por plancha, discriminado por type; máximo 1 por plancha y tipo';

COMMENT ON COLUMN indicolors.production_order_postpress_records.production_order_postpress_record_id IS 'Identificador único del registro de Terminados/Acabados';
COMMENT ON COLUMN indicolors.production_order_postpress_records.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_postpress_records.production_order_id IS 'Identificador de la Orden de Producción a la que pertenece';
COMMENT ON COLUMN indicolors.production_order_postpress_records.plate_id IS 'Identificador de la plancha a la que pertenece el registro (FK a production_order_plates)';
COMMENT ON COLUMN indicolors.production_order_postpress_records.type IS 'Tipo de registro: FINISHED_PRODUCT (Terminados) | FINISHING_PROCESS (Acabados)';
COMMENT ON COLUMN indicolors.production_order_postpress_records.completed IS 'True=el registro de Terminados/Acabados para esta plancha está finalizado';
COMMENT ON COLUMN indicolors.production_order_postpress_records.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_postpress_records.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_postpress_records TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_postpress_records TO indicolors_app;
