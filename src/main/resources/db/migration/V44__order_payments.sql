-- Abonos/pagos append-only (módulo Pedidos / Abonos).
-- Tipos de liquidación: abono | anticipo | retencion | reversion (Opción A, listo para FE).

CREATE TABLE IF NOT EXISTS indicolors.order_payments (
    order_payment_id      CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id            CHARACTER VARYING(64)       NOT NULL,
    payment_number        CHARACTER VARYING(32)       NOT NULL,
    production_order_id   CHARACTER VARYING(64)       NOT NULL,
    client_id             CHARACTER VARYING(64)       NOT NULL,

    payment_type          CHARACTER VARYING(16)       NOT NULL DEFAULT 'abono',
    amount                NUMERIC(14,2)               NOT NULL,
    payment_method        CHARACTER VARYING(32)       NOT NULL,
    reference             CHARACTER VARYING(100),
    reversed_payment_id   CHARACTER VARYING(64),

    withholding_type      CHARACTER VARYING(32),
    withholding_base      NUMERIC(14,2),
    withholding_rate      NUMERIC(8,4),
    certificate_ref       CHARACTER VARYING(100),

    -- Reserva para facturación electrónica (imputación futura; sin FK aún).
    invoice_id            CHARACTER VARYING(64),

    paid_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    registered_by         CHARACTER VARYING(64)       NOT NULL,
    notes                 TEXT,

    created_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT order_payments_pkey PRIMARY KEY (order_payment_id),
    CONSTRAINT order_payments_payment_number_company_unique UNIQUE (company_id, payment_number),
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
        CHECK (payment_type IN ('abono', 'anticipo', 'retencion', 'reversion')),
    CONSTRAINT order_payments_amount_check
        CHECK (amount > 0),
    CONSTRAINT order_payments_method_check
        CHECK (payment_method IN ('efectivo', 'transferencia', 'cheque', 'tarjeta', 'otro', 'retencion')),
    CONSTRAINT order_payments_reversion_ref_check
        CHECK (payment_type <> 'reversion' OR reversed_payment_id IS NOT NULL),
    CONSTRAINT order_payments_non_reversion_no_ref_check
        CHECK (payment_type = 'reversion' OR reversed_payment_id IS NULL),
    CONSTRAINT order_payments_retencion_method_check
        CHECK (payment_type <> 'retencion' OR payment_method = 'retencion'),
    CONSTRAINT order_payments_cash_method_check
        CHECK (payment_type NOT IN ('abono', 'anticipo')
              OR payment_method IN ('efectivo', 'transferencia', 'cheque', 'tarjeta', 'otro')),
    CONSTRAINT order_payments_retencion_fields_check
        CHECK (payment_type <> 'retencion' OR withholding_type IS NOT NULL),
    CONSTRAINT order_payments_withholding_type_check
        CHECK (withholding_type IS NULL
               OR withholding_type IN ('retefuente', 'reteiva', 'reteica', 'otro')),
    CONSTRAINT order_payments_withholding_base_check
        CHECK (withholding_base IS NULL OR withholding_base >= 0),
    CONSTRAINT order_payments_withholding_rate_check
        CHECK (withholding_rate IS NULL OR withholding_rate >= 0),
    CONSTRAINT order_payments_payment_number_format_check
        CHECK (payment_number ~ '^ABN-[0-9]+$')
);

CREATE INDEX IF NOT EXISTS idx_order_payments_company_id ON indicolors.order_payments (company_id);
CREATE INDEX IF NOT EXISTS idx_order_payments_order_time ON indicolors.order_payments (company_id, production_order_id, paid_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_payments_client_time ON indicolors.order_payments (company_id, client_id, paid_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_payments_payment_number ON indicolors.order_payments (company_id, payment_number);
CREATE INDEX IF NOT EXISTS idx_order_payments_invoice_id ON indicolors.order_payments (company_id, invoice_id)
    WHERE invoice_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_order_payments_reversed_once
    ON indicolors.order_payments (company_id, reversed_payment_id)
    WHERE reversed_payment_id IS NOT NULL;

COMMENT ON TABLE indicolors.order_payments IS
    'Bitácora append-only de liquidaciones (abono/anticipo/retencion) y reversiones contra una OP. Nunca UPDATE/DELETE; anular = nueva fila reversion';

COMMENT ON COLUMN indicolors.order_payments.order_payment_id IS 'Identificador único del movimiento de pago';
COMMENT ON COLUMN indicolors.order_payments.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.order_payments.payment_number IS 'Consecutivo corto por compañía (ABN-{n}), generado por el backend';
COMMENT ON COLUMN indicolors.order_payments.production_order_id IS 'Orden de Producción contra la que se liquida';
COMMENT ON COLUMN indicolors.order_payments.client_id IS 'Cliente snapshot desde production_orders.client_id';
COMMENT ON COLUMN indicolors.order_payments.payment_type IS
    'abono=caja | anticipo=saldo a favor/pasivo operativo | retencion=liquidación fiscal sin caja | reversion=anulación';
COMMENT ON COLUMN indicolors.order_payments.amount IS 'Monto del movimiento, siempre positivo; el signo lo aplica el trigger según payment_type';
COMMENT ON COLUMN indicolors.order_payments.payment_method IS
    'Canal de caja, o retencion cuando payment_type=retencion';
COMMENT ON COLUMN indicolors.order_payments.reference IS 'Número de transferencia, cheque o comprobante, si aplica';
COMMENT ON COLUMN indicolors.order_payments.reversed_payment_id IS
    'order_payment_id del movimiento que se anula; obligatorio si payment_type=reversion';
COMMENT ON COLUMN indicolors.order_payments.withholding_type IS
    'Tipo de retención sufrida (retefuente|reteiva|reteica|otro); obligatorio si payment_type=retencion';
COMMENT ON COLUMN indicolors.order_payments.withholding_base IS 'Base gravable usada para calcular la retención';
COMMENT ON COLUMN indicolors.order_payments.withholding_rate IS 'Porcentaje aplicado (ej. 2.5000 = 2.5%)';
COMMENT ON COLUMN indicolors.order_payments.certificate_ref IS 'Número/referencia del certificado de retención';
COMMENT ON COLUMN indicolors.order_payments.invoice_id IS
    'Reserva para imputación a factura electrónica (nullable hasta FE)';
COMMENT ON COLUMN indicolors.order_payments.paid_at IS 'Fecha/hora real del cobro/liquidación';
COMMENT ON COLUMN indicolors.order_payments.registered_by IS 'Usuario que registró el movimiento';
COMMENT ON COLUMN indicolors.order_payments.notes IS 'Nota libre asociada al movimiento';
COMMENT ON COLUMN indicolors.order_payments.created_at IS 'Fecha y hora de persistencia del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.order_payments TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.order_payments TO indicolors_app;
