-- Tabla production_order_billing_details (módulo production-orders).

CREATE TABLE IF NOT EXISTS indicolors.production_order_billing_details (
    production_order_id        CHARACTER VARYING(64)       NOT NULL,  -- mismo id que production_orders (1:1)
    company_id                 CHARACTER VARYING(64)       NOT NULL,

    billing_discount_type      CHARACTER VARYING(10),   -- descuento global
    billing_discount_value     NUMERIC(12,2),
    client_costing_mode        CHARACTER VARYING(10),  -- exact | volume
    client_discount_type       CHARACTER VARYING(12),  -- flujo exact
    client_discount_value      NUMERIC(12,2),
    client_profitability_type  CHARACTER VARYING(12),
    client_profitability_value NUMERIC(12,2),
    client_volume_costing      JSONB,                  -- flujo volume: 3 propuestas
    delivery_start_date        DATE,
    delivery_end_date          DATE,
    advance_percentage         NUMERIC(5,2)                DEFAULT 50,
    client_signature_name      CHARACTER VARYING(150),
    bank_account_id            CHARACTER VARYING(64),
    billing_completed_at       TIMESTAMP WITHOUT TIME ZONE,

    created_at                 TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_billing_details_pkey PRIMARY KEY (production_order_id),
    CONSTRAINT production_order_billing_details_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_billing_details_order_fk FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_billing_details_bank_account_fk FOREIGN KEY (bank_account_id) REFERENCES indicolors.bank_accounts (account_id),
    CONSTRAINT production_order_billing_details_client_costing_mode_check CHECK (client_costing_mode IS NULL OR client_costing_mode IN ('exact','volume'))
);

CREATE INDEX IF NOT EXISTS idx_production_order_billing_details_company_id ON indicolors.production_order_billing_details (company_id);
CREATE INDEX IF NOT EXISTS idx_production_order_billing_details_bank_account_id ON indicolors.production_order_billing_details (bank_account_id);

COMMENT ON TABLE indicolors.production_order_billing_details IS 'Detalle de Cobro, relación 1:1 con production_orders (misma PK)';

COMMENT ON COLUMN indicolors.production_order_billing_details.production_order_id IS 'Identificador de la Orden de Producción (mismo id que production_orders, relación 1:1)';
COMMENT ON COLUMN indicolors.production_order_billing_details.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_billing_details.billing_discount_type IS 'Tipo del descuento global de Cobro: % | $';
COMMENT ON COLUMN indicolors.production_order_billing_details.billing_discount_value IS 'Valor del descuento global de Cobro';
COMMENT ON COLUMN indicolors.production_order_billing_details.client_costing_mode IS 'Modo de costeo al cliente: exact | volume';
COMMENT ON COLUMN indicolors.production_order_billing_details.client_discount_type IS 'Tipo de descuento al cliente en el modo exact';
COMMENT ON COLUMN indicolors.production_order_billing_details.client_discount_value IS 'Valor de descuento al cliente en el modo exact';
COMMENT ON COLUMN indicolors.production_order_billing_details.client_profitability_type IS 'Tipo de rentabilidad al cliente en el modo exact';
COMMENT ON COLUMN indicolors.production_order_billing_details.client_profitability_value IS 'Valor de rentabilidad al cliente en el modo exact';
COMMENT ON COLUMN indicolors.production_order_billing_details.client_volume_costing IS 'JSONB con las 3 propuestas de cantidad/descuento/rentabilidad del flujo "volume"';
COMMENT ON COLUMN indicolors.production_order_billing_details.delivery_start_date IS 'Fecha inicial estimada de entrega';
COMMENT ON COLUMN indicolors.production_order_billing_details.delivery_end_date IS 'Fecha final estimada de entrega';
COMMENT ON COLUMN indicolors.production_order_billing_details.advance_percentage IS 'Porcentaje de anticipo solicitado al cliente (por defecto 50%)';
COMMENT ON COLUMN indicolors.production_order_billing_details.client_signature_name IS 'Nombre de la persona que firma/autoriza en representación del cliente';
COMMENT ON COLUMN indicolors.production_order_billing_details.bank_account_id IS 'Identificador de la cuenta bancaria usada para el cobro (FK a bank_accounts)';
COMMENT ON COLUMN indicolors.production_order_billing_details.billing_completed_at IS 'Fecha/hora en que se completó el paso de Cobro';
COMMENT ON COLUMN indicolors.production_order_billing_details.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_billing_details.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_billing_details TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_billing_details TO indicolors_app;
