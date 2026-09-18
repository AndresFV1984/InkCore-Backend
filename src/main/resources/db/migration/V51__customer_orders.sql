-- Cabecera de pedido comercial (1:1 con production_orders).
-- Distinto de production_orders (OP de planta) y de order_deliveries (ledger de entregas).
-- Se crea al pasar la OP de PENDING a un estado IN_PROGRESS*.
-- odp_number = ODP-{n} es la identidad visible del pedido.

CREATE TABLE IF NOT EXISTS indicolors.customer_orders (
    customer_order_id     CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id            CHARACTER VARYING(64)       NOT NULL,
    odp_number            CHARACTER VARYING(32)       NOT NULL,
    production_order_id   CHARACTER VARYING(64)       NOT NULL,
    client_id             CHARACTER VARYING(64)       NOT NULL,
    created_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    created_by            CHARACTER VARYING(64)       NOT NULL,

    CONSTRAINT customer_orders_pkey PRIMARY KEY (customer_order_id),
    CONSTRAINT customer_orders_odp_number_company_unique UNIQUE (company_id, odp_number),
    CONSTRAINT customer_orders_production_order_unique UNIQUE (production_order_id),
    CONSTRAINT customer_orders_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT customer_orders_production_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id),
    CONSTRAINT customer_orders_client_fk
        FOREIGN KEY (client_id) REFERENCES indicolors.clients (client_id),
    CONSTRAINT customer_orders_created_by_fk
        FOREIGN KEY (created_by) REFERENCES indicolors.users (user_id),
    CONSTRAINT customer_orders_odp_number_format_check
        CHECK (odp_number ~ '^ODP-[0-9]+$')
);

CREATE INDEX IF NOT EXISTS idx_customer_orders_company_id
    ON indicolors.customer_orders (company_id);
CREATE INDEX IF NOT EXISTS idx_customer_orders_client_id
    ON indicolors.customer_orders (company_id, client_id);

COMMENT ON TABLE indicolors.customer_orders IS
    'Cabecera de pedido comercial (1:1 con production_orders). Distinto de la OP de planta. Se crea cuando la OP entra a IN_PROGRESS*; no es el ledger de entregas';
COMMENT ON COLUMN indicolors.customer_orders.customer_order_id IS
    'Identificador único (UUID) del pedido comercial';
COMMENT ON COLUMN indicolors.customer_orders.company_id IS
    'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.customer_orders.odp_number IS
    'Consecutivo corto del pedido por compañía (ej. ODP-1, ODP-42), generado por el backend; único por compañía';
COMMENT ON COLUMN indicolors.customer_orders.production_order_id IS
    'OP de planta asociada (1:1, forzado por UNIQUE)';
COMMENT ON COLUMN indicolors.customer_orders.client_id IS
    'Cliente snapshot desde production_orders.client_id al crear el pedido';
COMMENT ON COLUMN indicolors.customer_orders.created_at IS
    'Fecha y hora de creación del pedido';
COMMENT ON COLUMN indicolors.customer_orders.created_by IS
    'Usuario que disparó la transición a progreso (y por tanto la creación del pedido)';

GRANT ALL PRIVILEGES ON TABLE indicolors.customer_orders TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.customer_orders TO indicolors_app;
