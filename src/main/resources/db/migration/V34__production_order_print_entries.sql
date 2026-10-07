-- Tabla production_order_print_entries (módulo production-orders).

CREATE TABLE IF NOT EXISTS indicolors.production_order_print_entries (
    production_order_print_entry_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                      CHARACTER VARYING(64)       NOT NULL,
    print_id                        CHARACTER VARYING(64)       NOT NULL,

    shots_ink_count                 SMALLINT                    NOT NULL,
    shots_inks                      JSONB                       NOT NULL,
    reverse_ink_count               SMALLINT                    NOT NULL,
    reverse_inks                    JSONB                       NOT NULL,

    -- Grupo Color básico
    basic_flip_type                 CHARACTER VARYING(20)       NOT NULL,  -- no-flip|gripper-flip|square-flip
    basic_thousand_rate_id          CHARACTER VARYING(64),
    basic_rate_name                 CHARACTER VARYING(150),
    basic_rate_price                NUMERIC(12,2),
    basic_rate_gripper_flip_price   NUMERIC(12,2),
    basic_rate_square_flip_price    NUMERIC(12,2),
    basic_calculated_thousands      NUMERIC(10,2),
    basic_printing_price            NUMERIC(12,2),

    -- Grupo Pantone
    pantone_flip_type               CHARACTER VARYING(20)       NOT NULL,
    client_supplies_pantone_ink     BOOLEAN,
    pantone_ink_charge_price        NUMERIC(12,2),
    pantone_thousand_rate_id        CHARACTER VARYING(64),
    pantone_rate_name               CHARACTER VARYING(150),
    pantone_rate_price              NUMERIC(12,2),
    pantone_rate_gripper_flip_price NUMERIC(12,2),
    pantone_rate_square_flip_price  NUMERIC(12,2),
    pantone_calculated_thousands    NUMERIC(10,2),
    pantone_printing_price          NUMERIC(12,2),

    created_at                      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_print_entries_pkey PRIMARY KEY (production_order_print_entry_id),
    CONSTRAINT production_order_print_entries_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_print_entries_print_fk FOREIGN KEY (print_id) REFERENCES indicolors.production_order_print (production_order_print_id) ON DELETE CASCADE,
    CONSTRAINT production_order_print_entries_basic_rate_fk FOREIGN KEY (basic_thousand_rate_id) REFERENCES indicolors.thousand_rates (thousand_rate_id),
    CONSTRAINT production_order_print_entries_pantone_rate_fk FOREIGN KEY (pantone_thousand_rate_id) REFERENCES indicolors.thousand_rates (thousand_rate_id),
    CONSTRAINT production_order_print_entries_basic_flip_check CHECK (basic_flip_type IN ('no-flip','gripper-flip','square-flip')),
    CONSTRAINT production_order_print_entries_pantone_flip_check CHECK (pantone_flip_type IN ('no-flip','gripper-flip','square-flip'))
);

CREATE INDEX IF NOT EXISTS idx_production_order_print_entries_company_id ON indicolors.production_order_print_entries (company_id);
CREATE INDEX IF NOT EXISTS idx_production_order_print_entries_print_id ON indicolors.production_order_print_entries (print_id);
CREATE INDEX IF NOT EXISTS idx_production_order_print_entries_basic_thousand_rate_id ON indicolors.production_order_print_entries (basic_thousand_rate_id);
CREATE INDEX IF NOT EXISTS idx_production_order_print_entries_pantone_thousand_rate_id ON indicolors.production_order_print_entries (pantone_thousand_rate_id);

COMMENT ON TABLE indicolors.production_order_print_entries IS 'Entradas de tiro/retiro por registro de impresión, 0..N por plancha';

COMMENT ON COLUMN indicolors.production_order_print_entries.production_order_print_entry_id IS 'Identificador único de la entrada de tiro/retiro';
COMMENT ON COLUMN indicolors.production_order_print_entries.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_print_entries.print_id IS 'Identificador del registro de impresión al que pertenece la entrada (FK a production_order_print)';
COMMENT ON COLUMN indicolors.production_order_print_entries.shots_ink_count IS 'Cantidad de tintas usadas en el tiro (frente)';
COMMENT ON COLUMN indicolors.production_order_print_entries.shots_inks IS 'JSONB: lista de tintas usadas en el tiro (frente)';
COMMENT ON COLUMN indicolors.production_order_print_entries.reverse_ink_count IS 'Cantidad de tintas usadas en el retiro (reverso)';
COMMENT ON COLUMN indicolors.production_order_print_entries.reverse_inks IS 'JSONB: lista de tintas usadas en el retiro (reverso)';
COMMENT ON COLUMN indicolors.production_order_print_entries.basic_flip_type IS 'Tipo de volteo del grupo Color básico: no-flip|gripper-flip|square-flip';
COMMENT ON COLUMN indicolors.production_order_print_entries.basic_thousand_rate_id IS 'Identificador de la tarifa por millar del grupo básico (FK a thousand_rates)';
COMMENT ON COLUMN indicolors.production_order_print_entries.basic_rate_name IS 'Snapshot del nombre de la tarifa del grupo básico al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_print_entries.basic_rate_price IS 'Snapshot del precio de la tarifa del grupo básico al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_print_entries.basic_rate_gripper_flip_price IS 'Snapshot de thousand_rates.gripper_flip_price al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_print_entries.basic_rate_square_flip_price IS 'Snapshot de thousand_rates.square_flip_price al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_print_entries.basic_calculated_thousands IS 'Millares calculados para el grupo básico, calculado en servidor';
COMMENT ON COLUMN indicolors.production_order_print_entries.basic_printing_price IS 'Precio de impresión del grupo básico, calculado en servidor';
COMMENT ON COLUMN indicolors.production_order_print_entries.pantone_flip_type IS 'Tipo de volteo del grupo Pantone: no-flip|gripper-flip|square-flip';
COMMENT ON COLUMN indicolors.production_order_print_entries.client_supplies_pantone_ink IS 'Decisión de suministro de tinta Pantone, capturada por entrada (no por plancha)';
COMMENT ON COLUMN indicolors.production_order_print_entries.pantone_ink_charge_price IS 'Precio cobrado por tinta Pantone, cuando el cliente no la suministra';
COMMENT ON COLUMN indicolors.production_order_print_entries.pantone_thousand_rate_id IS 'Identificador de la tarifa por millar del grupo Pantone (FK a thousand_rates)';
COMMENT ON COLUMN indicolors.production_order_print_entries.pantone_rate_name IS 'Snapshot del nombre de la tarifa del grupo Pantone al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_print_entries.pantone_rate_price IS 'Snapshot del precio de la tarifa del grupo Pantone al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_print_entries.pantone_rate_gripper_flip_price IS 'Snapshot de thousand_rates.gripper_flip_price para el grupo Pantone';
COMMENT ON COLUMN indicolors.production_order_print_entries.pantone_rate_square_flip_price IS 'Snapshot de thousand_rates.square_flip_price para el grupo Pantone';
COMMENT ON COLUMN indicolors.production_order_print_entries.pantone_calculated_thousands IS 'Millares calculados para el grupo Pantone, calculado en servidor';
COMMENT ON COLUMN indicolors.production_order_print_entries.pantone_printing_price IS 'Precio de impresión del grupo Pantone, calculado en servidor';
COMMENT ON COLUMN indicolors.production_order_print_entries.created_at IS 'Fecha y hora de creación del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_print_entries TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_print_entries TO indicolors_app;
