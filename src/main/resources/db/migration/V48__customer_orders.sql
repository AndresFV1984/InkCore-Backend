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
    'Identificador Ãºnico (UUID) del pedido comercial';
COMMENT ON COLUMN indicolors.customer_orders.company_id IS
    'Identificador de la empresa dueÃ±a del registro';
COMMENT ON COLUMN indicolors.customer_orders.odp_number IS
    'Consecutivo corto del pedido por compaÃ±Ã­a (ej. ODP-1, ODP-42), generado por el backend; Ãºnico por compaÃ±Ã­a';
COMMENT ON COLUMN indicolors.customer_orders.production_order_id IS
    'OP de planta asociada (1:1, forzado por UNIQUE)';
COMMENT ON COLUMN indicolors.customer_orders.client_id IS
    'Cliente snapshot desde production_orders.client_id al crear el pedido';
COMMENT ON COLUMN indicolors.customer_orders.created_at IS
    'Fecha y hora de creaciÃ³n del pedido';
COMMENT ON COLUMN indicolors.customer_orders.created_by IS
    'Usuario que disparÃ³ la transiciÃ³n a progreso (y por tanto la creaciÃ³n del pedido)';

GRANT ALL PRIVILEGES ON TABLE indicolors.customer_orders TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.customer_orders TO indicolors_app;

-- ---------------------------------------------------------------------------
-- Al INSERT en customer_orders: shell accounts_receivable con ABN (cxc null).
-- ---------------------------------------------------------------------------


-- ---------------------------------------------------------------------------
-- Al INSERT en customer_orders: shell accounts_receivable con ABN (cxc null).
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION indicolors.fn_ensure_abonos_account_on_customer_order()
RETURNS TRIGGER AS $$
DECLARE
    v_exists      BOOLEAN;
    v_term_days   INTEGER;
    v_total_units INTEGER;
BEGIN
    SELECT EXISTS (
        SELECT 1 FROM indicolors.accounts_receivable
        WHERE production_order_id = NEW.production_order_id
    ) INTO v_exists;

    IF v_exists THEN
        RETURN NEW;
    END IF;

    SELECT COALESCE(credit_days, 0) INTO v_term_days
    FROM indicolors.clients
    WHERE client_id = NEW.client_id;

    SELECT COALESCE(requested_quantity, 0) INTO v_total_units
    FROM indicolors.production_orders
    WHERE production_order_id = NEW.production_order_id;

    INSERT INTO indicolors.accounts_receivable (
        cxc_number, abonos_number, company_id, production_order_id, client_id,
        total_units, delivered_units, pending_units,
        total_owed, total_paid, total_remaining,
        total_cash_paid, total_withheld, total_advance_paid,
        opened_at, due_date, payment_term_days,
        status, updated_at
    )
    VALUES (
        NULL,
        indicolors.fn_next_abonos_number(NEW.company_id),
        NEW.company_id,
        NEW.production_order_id,
        NEW.client_id,
        COALESCE(v_total_units, 0),
        0,
        GREATEST(COALESCE(v_total_units, 0), 0),
        0, 0, 0,
        0, 0, 0,
        NULL, NULL, COALESCE(v_term_days, 0),
        'pendiente', now()
    );

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_customer_orders_ensure_abonos_account ON indicolors.customer_orders;
CREATE TRIGGER trg_customer_orders_ensure_abonos_account
    AFTER INSERT ON indicolors.customer_orders
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_ensure_abonos_account_on_customer_order();

COMMENT ON FUNCTION indicolors.fn_ensure_abonos_account_on_customer_order() IS
    'Tras crear pedido comercial: inserta shell accounts_receivable con abonos_number=ABN-{n} '
    '(cxc_number null hasta la 1Âª entrega). Idempotente por production_order_id.';
