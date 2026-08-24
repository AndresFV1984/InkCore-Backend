-- Tabla production_order_prepress_details (módulo production-orders).

CREATE TABLE IF NOT EXISTS indicolors.production_order_prepress_details (
    production_order_id      CHARACTER VARYING(64)       NOT NULL,  -- mismo id que production_orders (1:1)
    company_id               CHARACTER VARYING(64)       NOT NULL,

    is_new_design            BOOLEAN,
    design_name              CHARACTER VARYING(150),
    existing_design_order_id CHARACTER VARYING(64),   -- self-FK a production_orders
    has_design_cost          BOOLEAN                     NOT NULL DEFAULT FALSE,
    design_cost              NUMERIC(12,2),
    client_supplies_plates   BOOLEAN,
    client_plate_type        CHARACTER VARYING(20),  -- client-supplies|existing-plate|new-plate
    new_plate_cost           NUMERIC(12,2),
    assembly_price_id        CHARACTER VARYING(64),
    assembly_price_name      CHARACTER VARYING(150),
    assembly_price_cost      NUMERIC(12,2),
    die_cut_line             BOOLEAN                     NOT NULL DEFAULT FALSE,
    uv_reserve               BOOLEAN                     NOT NULL DEFAULT FALSE,
    stamping                 BOOLEAN                     NOT NULL DEFAULT FALSE,
    embossing                BOOLEAN                     NOT NULL DEFAULT FALSE,
    total_plates_value       NUMERIC(12,2),
    prepress_discount_type   CHARACTER VARYING(10),  -- % | $
    prepress_discount_value  NUMERIC(12,2),
    prepress_completed_at    TIMESTAMP WITHOUT TIME ZONE,

    created_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_prepress_details_pkey PRIMARY KEY (production_order_id),
    CONSTRAINT production_order_prepress_details_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_prepress_details_order_fk FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_prepress_details_existing_design_order_fk FOREIGN KEY (existing_design_order_id) REFERENCES indicolors.production_orders (production_order_id),
    CONSTRAINT production_order_prepress_details_assembly_price_fk FOREIGN KEY (assembly_price_id) REFERENCES indicolors.assembly_prices (assembly_price_id),
    CONSTRAINT production_order_prepress_details_client_plate_type_check CHECK (client_plate_type IS NULL OR client_plate_type IN ('client-supplies','existing-plate','new-plate'))
);

CREATE INDEX IF NOT EXISTS idx_production_order_prepress_details_company_id ON indicolors.production_order_prepress_details (company_id);
CREATE INDEX IF NOT EXISTS idx_production_order_prepress_details_existing_design_order ON indicolors.production_order_prepress_details (existing_design_order_id);
CREATE INDEX IF NOT EXISTS idx_production_order_prepress_details_assembly_price_id ON indicolors.production_order_prepress_details (assembly_price_id);

COMMENT ON TABLE indicolors.production_order_prepress_details IS 'Detalle de Preprensa, relación 1:1 con production_orders (misma PK)';

COMMENT ON COLUMN indicolors.production_order_prepress_details.production_order_id IS 'Identificador de la Orden de Producción (mismo id que production_orders, relación 1:1)';
COMMENT ON COLUMN indicolors.production_order_prepress_details.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_prepress_details.is_new_design IS 'True=diseño nuevo, False=diseño existente reutilizado de otra OP';
COMMENT ON COLUMN indicolors.production_order_prepress_details.design_name IS 'Nombre del diseño, cuando is_new_design=true';
COMMENT ON COLUMN indicolors.production_order_prepress_details.existing_design_order_id IS 'Self-FK: OP origen del diseño reutilizado';
COMMENT ON COLUMN indicolors.production_order_prepress_details.has_design_cost IS 'True=el diseño tiene un costo adicional a cobrar';
COMMENT ON COLUMN indicolors.production_order_prepress_details.design_cost IS 'Costo del diseño, cuando has_design_cost=true';
COMMENT ON COLUMN indicolors.production_order_prepress_details.client_supplies_plates IS 'True=el cliente suministra las planchas';
COMMENT ON COLUMN indicolors.production_order_prepress_details.client_plate_type IS 'Origen de la plancha cuando el cliente no la suministra: client-supplies|existing-plate|new-plate';
COMMENT ON COLUMN indicolors.production_order_prepress_details.new_plate_cost IS 'Costo de la plancha nueva, cuando client_plate_type=new-plate';
COMMENT ON COLUMN indicolors.production_order_prepress_details.assembly_price_id IS 'Identificador de la tarifa de armado seleccionada (FK a assembly_prices)';
COMMENT ON COLUMN indicolors.production_order_prepress_details.assembly_price_name IS 'Snapshot del nombre de la tarifa de armado al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_prepress_details.assembly_price_cost IS 'Snapshot del costo de la tarifa de armado al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_prepress_details.die_cut_line IS 'True=incluye línea de corte/troquel';
COMMENT ON COLUMN indicolors.production_order_prepress_details.uv_reserve IS 'True=incluye Reserva UV';
COMMENT ON COLUMN indicolors.production_order_prepress_details.stamping IS 'True=incluye Estampado';
COMMENT ON COLUMN indicolors.production_order_prepress_details.embossing IS 'True=incluye Repujado/Relieve';
COMMENT ON COLUMN indicolors.production_order_prepress_details.total_plates_value IS 'Valor total de planchas de la Orden, calculado en servidor';
COMMENT ON COLUMN indicolors.production_order_prepress_details.prepress_discount_type IS 'Tipo de descuento de Preprensa: % | $';
COMMENT ON COLUMN indicolors.production_order_prepress_details.prepress_discount_value IS 'Valor del descuento de Preprensa';
COMMENT ON COLUMN indicolors.production_order_prepress_details.prepress_completed_at IS 'Fecha/hora en que se completó el paso de Preprensa';
COMMENT ON COLUMN indicolors.production_order_prepress_details.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_prepress_details.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_prepress_details TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_prepress_details TO indicolors_app;
