-- Abonos/pagos append-only (mÃ³dulo Pedidos / Abonos).
-- Tipos de liquidaciÃ³n: abono | anticipo | retencion | reversion (OpciÃ³n A, listo para FE).

CREATE TABLE IF NOT EXISTS indicolors.order_payments (
    order_payment_id      CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id            CHARACTER VARYING(64)       NOT NULL,
    payment_number        CHARACTER VARYING(32)       NOT NULL,
    production_order_id   CHARACTER VARYING(64)       NOT NULL,
    client_id             CHARACTER VARYING(64)       NOT NULL,

    payment_type          CHARACTER VARYING(16)       NOT NULL DEFAULT 'abono',
    payment_concept       CHARACTER VARYING(16)       NOT NULL DEFAULT 'entrega',
    amount                NUMERIC(14,2)               NOT NULL,
    payment_method        CHARACTER VARYING(32)       NOT NULL,
    reference             CHARACTER VARYING(100),
    reversed_payment_id   CHARACTER VARYING(64),

    withholding_type      CHARACTER VARYING(32),
    withholding_base      NUMERIC(14,2),
    withholding_rate      NUMERIC(8,4),
    certificate_ref       CHARACTER VARYING(100),

    -- Reserva para facturaciÃ³n electrÃ³nica (imputaciÃ³n futura; sin FK aÃºn).
    -- Distinto de order_cxc_documents (CxC = factura de cartera).
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
    CONSTRAINT order_payments_payment_concept_check
        CHECK (payment_concept IN ('entrega', 'extra')),
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
        CHECK (payment_number ~ '^LIQ-[0-9]+$')
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
    'BitÃ¡cora append-only de liquidaciones (abono/anticipo/retencion) y reversiones contra una OP. Nunca UPDATE/DELETE; anular = nueva fila reversion';

COMMENT ON COLUMN indicolors.order_payments.order_payment_id IS 'Identificador Ãºnico del movimiento de pago';
COMMENT ON COLUMN indicolors.order_payments.company_id IS 'Identificador de la empresa dueÃ±a del registro';
COMMENT ON COLUMN indicolors.order_payments.payment_number IS
    'Consecutivo corto por compaÃ±Ã­a (LIQ-{n}), generado por el backend. '
    'Serie distinta de abonos_number (ABN-{n}) del agregado.';
COMMENT ON COLUMN indicolors.order_payments.production_order_id IS 'Orden de ProducciÃ³n contra la que se liquida';
COMMENT ON COLUMN indicolors.order_payments.client_id IS 'Cliente snapshot desde production_orders.client_id';
COMMENT ON COLUMN indicolors.order_payments.payment_type IS
    'abono=caja | anticipo=saldo a favor/pasivo operativo | retencion=liquidaciÃ³n fiscal sin caja | reversion=anulaciÃ³n';
COMMENT ON COLUMN indicolors.order_payments.payment_concept IS
    'Concepto de aplicaciÃ³n del pago: entrega (valor ENT/CxC entrega) o extra (ADN/CxC extra). Default entrega.';
COMMENT ON COLUMN indicolors.order_payments.amount IS 'Monto del movimiento, siempre positivo; el signo lo aplica el trigger segÃºn payment_type';
COMMENT ON COLUMN indicolors.order_payments.payment_method IS
    'Canal de caja, o retencion cuando payment_type=retencion';
COMMENT ON COLUMN indicolors.order_payments.reference IS 'NÃºmero de transferencia, cheque o comprobante, si aplica';
COMMENT ON COLUMN indicolors.order_payments.reversed_payment_id IS
    'order_payment_id del movimiento que se anula; obligatorio si payment_type=reversion';
COMMENT ON COLUMN indicolors.order_payments.withholding_type IS
    'Tipo de retenciÃ³n sufrida (retefuente|reteiva|reteica|otro); obligatorio si payment_type=retencion';
COMMENT ON COLUMN indicolors.order_payments.withholding_base IS 'Base gravable usada para calcular la retenciÃ³n';
COMMENT ON COLUMN indicolors.order_payments.withholding_rate IS 'Porcentaje aplicado (ej. 2.5000 = 2.5%)';
COMMENT ON COLUMN indicolors.order_payments.certificate_ref IS 'NÃºmero/referencia del certificado de retenciÃ³n';
COMMENT ON COLUMN indicolors.order_payments.invoice_id IS
    'Reserva para imputaciÃ³n a factura electrÃ³nica (nullable hasta FE)';
COMMENT ON COLUMN indicolors.order_payments.paid_at IS 'Fecha/hora real del cobro/liquidaciÃ³n';
COMMENT ON COLUMN indicolors.order_payments.registered_by IS 'Usuario que registrÃ³ el movimiento';
COMMENT ON COLUMN indicolors.order_payments.notes IS 'Nota libre asociada al movimiento';
COMMENT ON COLUMN indicolors.order_payments.created_at IS 'Fecha y hora de persistencia del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.order_payments TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.order_payments TO indicolors_app;

-- ---------------------------------------------------------------------------
-- Al INSERT en customer_orders: shell accounts_receivable con ABN (cxc null).
-- ---------------------------------------------------------------------------


-- ---------------------------------------------------------------------------
-- Sync pago: no crea CXC; usa ABN existente o crea shell con cxc null.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION indicolors.fn_sync_accounts_receivable_payment()
RETURNS TRIGGER AS $$
DECLARE
    v_signed_amount         NUMERIC(14,2);
    v_exists                BOOLEAN;
    v_orig_type             CHARACTER VARYING(16);
    v_bucket_type           CHARACTER VARYING(16);
    v_cash_delta            NUMERIC(14,2) := 0;
    v_withheld_delta        NUMERIC(14,2) := 0;
    v_advance_delta         NUMERIC(14,2) := 0;
    v_term_days             INTEGER;
    v_last_payment_number   CHARACTER VARYING(32);
    v_last_payment_at       TIMESTAMP WITHOUT TIME ZONE;
    v_total_units           INTEGER;
BEGIN
    v_signed_amount := CASE WHEN NEW.payment_type = 'reversion' THEN -NEW.amount ELSE NEW.amount END;

    IF NEW.payment_type = 'reversion' THEN
        SELECT payment_type INTO v_orig_type
        FROM indicolors.order_payments
        WHERE order_payment_id = NEW.reversed_payment_id;
        v_bucket_type := COALESCE(v_orig_type, 'abono');

        SELECT p.payment_number, p.paid_at
        INTO v_last_payment_number, v_last_payment_at
        FROM indicolors.order_payments p
        WHERE p.production_order_id = NEW.production_order_id
          AND p.payment_type <> 'reversion'
          AND NOT EXISTS (
              SELECT 1
              FROM indicolors.order_payments r
              WHERE r.reversed_payment_id = p.order_payment_id
                AND r.payment_type = 'reversion'
          )
        ORDER BY p.paid_at DESC, p.created_at DESC
        LIMIT 1;
    ELSE
        v_bucket_type := NEW.payment_type;
        v_last_payment_number := NEW.payment_number;
        v_last_payment_at := NEW.paid_at;
    END IF;

    IF v_bucket_type = 'retencion' THEN
        v_withheld_delta := v_signed_amount;
    ELSIF v_bucket_type = 'anticipo' THEN
        v_advance_delta := v_signed_amount;
    ELSE
        v_cash_delta := v_signed_amount;
    END IF;

    SELECT EXISTS (
        SELECT 1 FROM indicolors.accounts_receivable WHERE production_order_id = NEW.production_order_id
    ) INTO v_exists;

    IF NOT v_exists THEN
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
            status, last_payment_number, last_payment_at, updated_at
        )
        VALUES (
            NULL,
            indicolors.fn_next_abonos_number(NEW.company_id),
            NEW.company_id, NEW.production_order_id, NEW.client_id,
            COALESCE(v_total_units, 0), 0, GREATEST(COALESCE(v_total_units, 0), 0),
            0, v_signed_amount, -v_signed_amount,
            v_cash_delta, v_withheld_delta, v_advance_delta,
            NULL, NULL, COALESCE(v_term_days, 0),
            'pendiente', v_last_payment_number, v_last_payment_at, now()
        );
    ELSE
        UPDATE indicolors.accounts_receivable SET
            total_paid           = total_paid + v_signed_amount,
            total_remaining      = total_owed - (total_paid + v_signed_amount),
            total_cash_paid      = total_cash_paid + v_cash_delta,
            total_withheld       = total_withheld + v_withheld_delta,
            total_advance_paid   = total_advance_paid + v_advance_delta,
            last_payment_number  = v_last_payment_number,
            last_payment_at      = v_last_payment_at,
            updated_at           = now(),
            status = CASE
                WHEN delivered_units = 0 AND total_owed = 0
                     AND (total_paid + v_signed_amount) = 0 THEN 'anulado'
                WHEN total_owed > 0
                     AND (total_paid + v_signed_amount) >= total_owed THEN 'pagado'
                WHEN (total_paid + v_signed_amount) > 0 THEN 'parcial'
                ELSE 'pendiente'
            END
        WHERE production_order_id = NEW.production_order_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_payments_sync_accounts_receivable ON indicolors.order_payments;
CREATE TRIGGER trg_payments_sync_accounts_receivable
    AFTER INSERT ON indicolors.order_payments
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_sync_accounts_receivable_payment();

COMMENT ON FUNCTION indicolors.fn_sync_accounts_receivable_payment() IS
    'Upsert CxC/Abonos tras liquidaciÃ³n LIQ; no asigna cxc_number (queda null hasta 1Âª entrega); '
    'actualiza last_payment_* y no regenera abonos_number';
