-- Triggers del módulo Pedidos / CxC / Abonos (sin CREATE TABLE).
-- Estrategia CXC: el trigger asigna accounts_receivable_id (DEFAULT) + cxc_number vía fn_next_cxc_number
-- solo en el primer INSERT; los UPDATE posteriores nunca regeneran esos campos.
-- due_date / payment_term_days se fijan al abrir deuda (1ª entrega) desde clients.credit_days.
-- Depende de: order_deliveries, order_payments, accounts_receivable, station_order_progress,
-- clients, accounts_receivable_number_sequences / fn_next_cxc_number.

CREATE OR REPLACE FUNCTION indicolors.fn_validate_delivery()
RETURNS TRIGGER AS $$
DECLARE
    v_processed     INTEGER;
    v_delivered     INTEGER;
    v_available     INTEGER;
    v_orig          indicolors.order_deliveries%ROWTYPE;
    v_total_owed    NUMERIC(14,2);
    v_total_paid    NUMERIC(14,2);
BEGIN
    IF NEW.movement_type = 'entrega' THEN
        SELECT cantidad_disponible INTO v_processed
        FROM indicolors.station_order_progress
        WHERE production_order_id = NEW.production_order_id;

        SELECT delivered_units INTO v_delivered
        FROM indicolors.accounts_receivable
        WHERE production_order_id = NEW.production_order_id;

        v_available := COALESCE(v_processed, 0) - COALESCE(v_delivered, 0);
        NEW.available_before := v_available;

        IF NEW.quantity_delivered > v_available THEN
            RAISE EXCEPTION
                'No se puede entregar % unidades: solo hay % disponibles para la OP %',
                NEW.quantity_delivered, v_available, NEW.production_order_id;
        END IF;
    ELSE
        SELECT * INTO v_orig
        FROM indicolors.order_deliveries
        WHERE order_delivery_id = NEW.reversed_delivery_id;

        IF NOT FOUND THEN
            RAISE EXCEPTION 'La entrega a anular % no existe', NEW.reversed_delivery_id;
        END IF;

        IF v_orig.movement_type <> 'entrega' THEN
            RAISE EXCEPTION 'Solo se puede anular una entrega original, no otra reversión (%)', NEW.reversed_delivery_id;
        END IF;

        IF v_orig.production_order_id <> NEW.production_order_id
           OR v_orig.company_id <> NEW.company_id THEN
            RAISE EXCEPTION 'La reversión debe pertenecer a la misma OP y compañía que la entrega original %', NEW.reversed_delivery_id;
        END IF;

        IF EXISTS (
            SELECT 1 FROM indicolors.order_deliveries
            WHERE reversed_delivery_id = NEW.reversed_delivery_id
        ) THEN
            RAISE EXCEPTION 'La entrega % ya fue anulada previamente', NEW.reversed_delivery_id;
        END IF;

        IF NEW.quantity_delivered <> v_orig.quantity_delivered
           OR NEW.total_value <> v_orig.total_value THEN
            RAISE EXCEPTION
                'La reversión debe anular exactamente la entrega original: % unidades por %',
                v_orig.quantity_delivered, v_orig.total_value;
        END IF;

        SELECT total_owed, total_paid INTO v_total_owed, v_total_paid
        FROM indicolors.accounts_receivable
        WHERE production_order_id = NEW.production_order_id;

        IF (COALESCE(v_total_owed, 0) - NEW.total_value) < COALESCE(v_total_paid, 0) THEN
            RAISE EXCEPTION
                'No se puede anular la entrega %: el saldo adeudado quedaría (%) por debajo de lo ya abonado (%)',
                NEW.reversed_delivery_id, (COALESCE(v_total_owed, 0) - NEW.total_value), v_total_paid;
        END IF;

        SELECT cantidad_disponible INTO v_processed
        FROM indicolors.station_order_progress
        WHERE production_order_id = NEW.production_order_id;

        SELECT delivered_units INTO v_delivered
        FROM indicolors.accounts_receivable
        WHERE production_order_id = NEW.production_order_id;

        NEW.available_before := COALESCE(v_processed, 0) - COALESCE(v_delivered, 0);
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_deliveries_validate ON indicolors.order_deliveries;
CREATE TRIGGER trg_deliveries_validate
    BEFORE INSERT ON indicolors.order_deliveries
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_validate_delivery();

COMMENT ON FUNCTION indicolors.fn_validate_delivery() IS
    'entrega: calcula available_before y rechaza si quantity_delivered supera lo disponible. reversion: valida entrega original y saldo vs abonos';

CREATE OR REPLACE FUNCTION indicolors.fn_sync_accounts_receivable_delivery()
RETURNS TRIGGER AS $$
DECLARE
    v_total_units INTEGER;
    v_exists      BOOLEAN;
    v_sign        INTEGER;
    v_term_days   INTEGER;
BEGIN
    v_sign := CASE WHEN NEW.movement_type = 'reversion' THEN -1 ELSE 1 END;

    SELECT requested_quantity INTO v_total_units
    FROM indicolors.production_orders
    WHERE production_order_id = NEW.production_order_id;

    SELECT EXISTS (
        SELECT 1 FROM indicolors.accounts_receivable WHERE production_order_id = NEW.production_order_id
    ) INTO v_exists;

    IF NOT v_exists THEN
        SELECT COALESCE(credit_days, 0) INTO v_term_days
        FROM indicolors.clients
        WHERE client_id = NEW.client_id;

        INSERT INTO indicolors.accounts_receivable (
            cxc_number, company_id, production_order_id, client_id,
            total_units, delivered_units, pending_units,
            total_owed, total_paid, total_remaining,
            total_cash_paid, total_withheld, total_advance_paid,
            opened_at, due_date, payment_term_days,
            status, last_delivery_at, updated_at
        )
        VALUES (
            indicolors.fn_next_cxc_number(NEW.company_id),
            NEW.company_id, NEW.production_order_id, NEW.client_id,
            COALESCE(v_total_units, NEW.quantity_delivered),
            NEW.quantity_delivered,
            GREATEST(COALESCE(v_total_units, NEW.quantity_delivered) - NEW.quantity_delivered, 0),
            NEW.total_value, 0, NEW.total_value,
            0, 0, 0,
            NEW.delivered_at,
            (NEW.delivered_at::date + COALESCE(v_term_days, 0)),
            COALESCE(v_term_days, 0),
            'pendiente', NEW.delivered_at, now()
        );
    ELSE
        UPDATE indicolors.accounts_receivable SET
            delivered_units  = delivered_units + v_sign * NEW.quantity_delivered,
            pending_units    = GREATEST(total_units - (delivered_units + v_sign * NEW.quantity_delivered), 0),
            total_owed       = total_owed + v_sign * NEW.total_value,
            total_remaining  = (total_owed + v_sign * NEW.total_value) - total_paid,
            opened_at        = CASE
                WHEN opened_at IS NULL AND NEW.movement_type = 'entrega' THEN NEW.delivered_at
                ELSE opened_at
            END,
            due_date         = CASE
                WHEN due_date IS NULL AND NEW.movement_type = 'entrega' THEN
                    (NEW.delivered_at::date + payment_term_days)
                ELSE due_date
            END,
            last_delivery_at = CASE WHEN NEW.movement_type = 'entrega' THEN NEW.delivered_at ELSE last_delivery_at END,
            updated_at       = now(),
            status = CASE
                WHEN (delivered_units + v_sign * NEW.quantity_delivered) = 0
                     AND (total_owed + v_sign * NEW.total_value) = 0
                     AND total_paid = 0 THEN 'anulado'
                WHEN total_paid >= (total_owed + v_sign * NEW.total_value)
                     AND (total_owed + v_sign * NEW.total_value) > 0 THEN 'pagado'
                WHEN total_paid > 0 THEN 'parcial'
                ELSE 'pendiente'
            END
        WHERE production_order_id = NEW.production_order_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_deliveries_sync_accounts_receivable ON indicolors.order_deliveries;
CREATE TRIGGER trg_deliveries_sync_accounts_receivable
    AFTER INSERT ON indicolors.order_deliveries
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_sync_accounts_receivable_delivery();

COMMENT ON FUNCTION indicolors.fn_sync_accounts_receivable_delivery() IS
    'Upsert CxC tras entrega/reversión; fija opened_at/due_date/payment_term_days solo en apertura (opened_at nunca se reescribe ni se limpia)';

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

        INSERT INTO indicolors.accounts_receivable (
            cxc_number, company_id, production_order_id, client_id,
            total_units, delivered_units, pending_units,
            total_owed, total_paid, total_remaining,
            total_cash_paid, total_withheld, total_advance_paid,
            opened_at, due_date, payment_term_days,
            status, last_payment_number, last_payment_at, updated_at
        )
        VALUES (
            indicolors.fn_next_cxc_number(NEW.company_id),
            NEW.company_id, NEW.production_order_id, NEW.client_id,
            0, 0, 0, 0, v_signed_amount, -v_signed_amount,
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
    'Upsert CxC tras abono/anticipo/retencion/reversion; actualiza total_paid, desglose y last_payment_* del último vigente';

-- Backfills defensivos (idempotentes) del agregado CxC; viven aquí (no en V46) para no mezclar DML con el DDL de la tabla.
UPDATE indicolors.accounts_receivable ar
SET opened_at = src.first_delivered_at
FROM (
    SELECT
        d.production_order_id,
        MIN(d.delivered_at) AS first_delivered_at
    FROM indicolors.order_deliveries d
    WHERE d.movement_type = 'entrega'
      AND NOT EXISTS (
          SELECT 1
          FROM indicolors.order_deliveries r
          WHERE r.reversed_delivery_id = d.order_delivery_id
            AND r.movement_type = 'reversion'
      )
    GROUP BY d.production_order_id
) src
WHERE ar.production_order_id = src.production_order_id
  AND ar.opened_at IS NULL;

UPDATE indicolors.accounts_receivable ar
SET
    last_payment_number = src.payment_number,
    last_payment_at     = src.paid_at
FROM (
    SELECT DISTINCT ON (p.production_order_id)
        p.production_order_id,
        p.payment_number,
        p.paid_at
    FROM indicolors.order_payments p
    WHERE p.payment_type <> 'reversion'
      AND NOT EXISTS (
          SELECT 1
          FROM indicolors.order_payments r
          WHERE r.reversed_payment_id = p.order_payment_id
            AND r.payment_type = 'reversion'
      )
    ORDER BY p.production_order_id, p.paid_at DESC, p.created_at DESC
) src
WHERE ar.production_order_id = src.production_order_id;

UPDATE indicolors.accounts_receivable ar
SET
    last_payment_number = NULL,
    last_payment_at     = NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM indicolors.order_payments p
    WHERE p.production_order_id = ar.production_order_id
      AND p.payment_type <> 'reversion'
      AND NOT EXISTS (
          SELECT 1
          FROM indicolors.order_payments r
          WHERE r.reversed_payment_id = p.order_payment_id
            AND r.payment_type = 'reversion'
      )
);
