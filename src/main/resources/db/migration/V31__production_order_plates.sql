-- Tabla production_order_plates (módulo production-orders).

CREATE TABLE IF NOT EXISTS indicolors.production_order_plates (
    production_order_plate_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                CHARACTER VARYING(64)       NOT NULL,
    production_order_id       CHARACTER VARYING(64)       NOT NULL,

    colors                    CHARACTER VARYING(20)       NOT NULL,
    plate_type_id             CHARACTER VARYING(64),
    plate_name                CHARACTER VARYING(80),
    plate_size                CHARACTER VARYING(30),
    plate_price               NUMERIC(12,2),

    quantity                  INTEGER                     NOT NULL,
    cavities                  SMALLINT,
    good_sizes                INTEGER,
    surplus                   INTEGER,
    plates_count              SMALLINT,
    total_value               NUMERIC(12,2),

    detail                    CHARACTER VARYING(100),
    observation               TEXT,
    manual_entry              BOOLEAN                     NOT NULL DEFAULT FALSE,
    plate_supply              CHARACTER VARYING(20),

    plate_replacement         BOOLEAN                     NOT NULL DEFAULT FALSE,
    replacement_quantity      INTEGER,

    created_at                TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_plates_pkey PRIMARY KEY (production_order_plate_id),
    CONSTRAINT production_order_plates_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_plates_order_fk FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_plates_plate_type_fk FOREIGN KEY (plate_type_id) REFERENCES indicolors.plate_types (plate_type_id),
    CONSTRAINT production_order_plates_quantity_check CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_production_order_plates_company_id ON indicolors.production_order_plates (company_id);
CREATE INDEX IF NOT EXISTS idx_production_order_plates_order_id ON indicolors.production_order_plates (production_order_id);
CREATE INDEX IF NOT EXISTS idx_production_order_plates_plate_type_id ON indicolors.production_order_plates (plate_type_id);

COMMENT ON TABLE indicolors.production_order_plates IS 'Planchas configuradas en Preprensa, 0..N por Orden de Producción';

COMMENT ON COLUMN indicolors.production_order_plates.production_order_plate_id IS 'Identificador único de la plancha';
COMMENT ON COLUMN indicolors.production_order_plates.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_plates.production_order_id IS 'Identificador de la Orden de Producción a la que pertenece la plancha';
COMMENT ON COLUMN indicolors.production_order_plates.colors IS 'Cantidad/categoría de colores de la plancha (ej. 1 COLOR, 4 COLORES)';
COMMENT ON COLUMN indicolors.production_order_plates.plate_type_id IS 'Identificador del tipo de plancha seleccionado (FK a plate_types)';
COMMENT ON COLUMN indicolors.production_order_plates.plate_name IS 'Snapshot del nombre del tipo de plancha al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_plates.plate_size IS 'Snapshot de la medida del tipo de plancha al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_plates.plate_price IS 'Snapshot del precio del tipo de plancha al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_plates.quantity IS 'Cantidad solicitada para la plancha';
COMMENT ON COLUMN indicolors.production_order_plates.cavities IS 'Cantidad de cavidades del troquel/plancha';
COMMENT ON COLUMN indicolors.production_order_plates.good_sizes IS 'Calculado en servidor = quantity / cavities';
COMMENT ON COLUMN indicolors.production_order_plates.surplus IS 'Excedente calculado para la plancha';
COMMENT ON COLUMN indicolors.production_order_plates.plates_count IS 'Cantidad de planchas físicas requeridas';
COMMENT ON COLUMN indicolors.production_order_plates.total_value IS 'Calculado en servidor';
COMMENT ON COLUMN indicolors.production_order_plates.detail IS 'Detalle adicional de la plancha';
COMMENT ON COLUMN indicolors.production_order_plates.observation IS 'Observaciones libres sobre la plancha';
COMMENT ON COLUMN indicolors.production_order_plates.manual_entry IS 'True=los valores de la plancha se ingresaron manualmente, no desde catálogo';
COMMENT ON COLUMN indicolors.production_order_plates.plate_supply IS 'Origen de suministro de la plancha';
COMMENT ON COLUMN indicolors.production_order_plates.plate_replacement IS 'True=esta plancha es una reposición';
COMMENT ON COLUMN indicolors.production_order_plates.replacement_quantity IS 'Cantidad de reposición, cuando plate_replacement=true';
COMMENT ON COLUMN indicolors.production_order_plates.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_plates.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_plates TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_plates TO indicolors_app;
