-- Abonos/pagos append-only (módulo Pedidos / Abonos).

CREATE TABLE IF NOT EXISTS indicolors.order_payments (
    order_payment_id      CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id            CHARACTER VARYING(64)       NOT NULL,
    production_order_id   CHARACTER VARYING(64)       NOT NULL,
    client_id             CHARACTER VARYING(64)       NOT NULL,

    payment_type          CHARACTER VARYING(16)       NOT NULL DEFAULT 'abono',
    amount                NUMERIC(14,2)               NOT NULL,
    payment_method        CHARACTER VARYING(32)       NOT NULL,
    reference             CHARACTER VARYING(100),
    reversed_payment_id   CHARACTER VARYING(64),

    paid_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    registered_by         CHARACTER VARYING(64)       NOT NULL,
    notes                 TEXT,

    created_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT order_payments_pkey PRIMARY KEY (order_payment_id),
    CONSTRAINT order_payments_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT order_payments_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id),
    CONSTRAINT order_payments_client_fk
        FOREIGN KEY (client_id) REFERENCES indicolors.clients (client_id),
    CONSTRAINT order_payments_registered_by_fk
        FOREIGN KEY (registered_by) REFERENCES indicolors.users (user_id),
    CONSTRAINT order_payments_reversed_fk
        FOREIGN KEY (reversed_payment_id) REFERENCES indicolors.order_payments (order_payment_id),
    CONSTRAINT order_payments_type_check
        CHECK (payment_type IN ('abono', 'reversion')),
    CONSTRAINT order_payments_amount_check
        CHECK (amount > 0),
    CONSTRAINT order_payments_method_check
        CHECK (payment_method IN ('efectivo', 'transferencia', 'cheque', 'tarjeta', 'otro')),
    CONSTRAINT order_payments_reversion_ref_check
        CHECK (payment_type <> 'reversion' OR reversed_payment_id IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_order_payments_company_id ON indicolors.order_payments (company_id);
CREATE INDEX IF NOT EXISTS idx_order_payments_order_time ON indicolors.order_payments (company_id, production_order_id, paid_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_payments_client_time ON indicolors.order_payments (company_id, client_id, paid_at DESC);

COMMENT ON TABLE indicolors.order_payments IS 'Bitácora append-only de abonos/pagos de un cliente contra una OP. Un abono nunca se edita ni se borra; se anula insertando una fila payment_type=reversion que referencia al abono original';

COMMENT ON COLUMN indicolors.order_payments.order_payment_id IS 'Identificador único del movimiento de pago';
COMMENT ON COLUMN indicolors.order_payments.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.order_payments.production_order_id IS 'Orden de Producción contra la que se abona';
COMMENT ON COLUMN indicolors.order_payments.client_id IS 'Cliente que realiza el abono (snapshot desde production_orders.client_id)';
COMMENT ON COLUMN indicolors.order_payments.payment_type IS 'abono=pago normal | reversion=anulación de un abono previo';
COMMENT ON COLUMN indicolors.order_payments.amount IS 'Monto del movimiento, siempre positivo; el signo lo aplica el trigger según payment_type';
COMMENT ON COLUMN indicolors.order_payments.payment_method IS 'Medio de pago usado';
COMMENT ON COLUMN indicolors.order_payments.reference IS 'Número de transferencia, cheque o comprobante, si aplica';
COMMENT ON COLUMN indicolors.order_payments.reversed_payment_id IS 'order_payment_id del abono que se está anulando; obligatorio si payment_type=reversion';
COMMENT ON COLUMN indicolors.order_payments.paid_at IS 'Fecha/hora real del pago';
COMMENT ON COLUMN indicolors.order_payments.registered_by IS 'Usuario que registró el abono';
COMMENT ON COLUMN indicolors.order_payments.notes IS 'Nota libre asociada al movimiento';
COMMENT ON COLUMN indicolors.order_payments.created_at IS 'Fecha y hora de persistencia del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.order_payments TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.order_payments TO indicolors_app;
