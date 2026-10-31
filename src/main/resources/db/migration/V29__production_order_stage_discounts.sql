-- Tabla production_order_stage_discounts (módulo production-orders).

CREATE TABLE IF NOT EXISTS indicolors.production_order_stage_discounts (
    production_order_stage_discount_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                         CHARACTER VARYING(64)       NOT NULL,
    production_order_id                CHARACTER VARYING(64)       NOT NULL,
    stage                              CHARACTER VARYING(20)       NOT NULL,  -- CUTTING|FINISHED_PRODUCTS|FINISHING_PROCESSES (Preprensa y Cobro tienen su propio descuento en su tabla 1:1)
    discount_type                      CHARACTER VARYING(10),  -- % | $
    discount_value                     NUMERIC(12,2),

    created_at                         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_stage_discounts_pkey PRIMARY KEY (production_order_stage_discount_id),
    CONSTRAINT production_order_stage_discounts_order_stage_unique UNIQUE (production_order_id, stage),
    CONSTRAINT production_order_stage_discounts_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_stage_discounts_order_fk FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_stage_discounts_stage_check CHECK (stage IN ('CUTTING','FINISHED_PRODUCTS','FINISHING_PROCESSES')),
    CONSTRAINT production_order_stage_discounts_discount_type_check CHECK (discount_type IS NULL OR discount_type IN ('%','$'))
);

CREATE INDEX IF NOT EXISTS idx_production_order_stage_discounts_company_id ON indicolors.production_order_stage_discounts (company_id);
CREATE INDEX IF NOT EXISTS idx_production_order_stage_discounts_order_id ON indicolors.production_order_stage_discounts (production_order_id);

COMMENT ON TABLE indicolors.production_order_stage_discounts IS 'Descuento configurado por etapa (Corte, Terminados, Acabados); máximo 1 por etapa (UNIQUE order_id, stage). Preprensa y Cobro tienen su propio descuento dentro de production_order_prepress_details/billing_details por no repetirse';

COMMENT ON COLUMN indicolors.production_order_stage_discounts.production_order_stage_discount_id IS 'Identificador único del descuento por etapa';
COMMENT ON COLUMN indicolors.production_order_stage_discounts.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_stage_discounts.production_order_id IS 'Identificador de la Orden de Producción a la que pertenece el descuento';
COMMENT ON COLUMN indicolors.production_order_stage_discounts.stage IS 'Etapa a la que corresponde el descuento: CUTTING|FINISHED_PRODUCTS|FINISHING_PROCESSES';
COMMENT ON COLUMN indicolors.production_order_stage_discounts.discount_type IS 'Tipo de descuento de la etapa: % | $';
COMMENT ON COLUMN indicolors.production_order_stage_discounts.discount_value IS 'Valor del descuento de la etapa';
COMMENT ON COLUMN indicolors.production_order_stage_discounts.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_stage_discounts.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_stage_discounts TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_stage_discounts TO indicolors_app;
