-- Triggers del módulo Pedidos / CxC / Abonos (sin CREATE TABLE).
-- Depende de: order_deliveries, order_payments, ar_summary, station_order_progress.

CREATE OR REPLACE FUNCTION indicolors.fn_validate_delivery()
RETURNS TRIGGER AS $$
DECLARE
    v_processed   INTEGER;
    v_delivered   INTEGER;
    v_available   INTEGER;
BEGIN
    SELECT cantidad_disponible INTO v_processed
    FROM indicolors.station_order_progress
    WHERE production_order_id = NEW.production_order_id;

    SELECT delivered_units INTO v_delivered
    FROM indicolors.ar_summary
    WHERE production_order_id = NEW.production_order_id;

    v_available := COALESCE(v_processed, 0) - COALESCE(v_delivered, 0);
    NEW.available_before := v_available;

    IF NEW.quantity_delivered > v_available THEN
        RAISE EXCEPTION
            'No se puede entregar % unidades: solo hay % disponibles para la OP %',
            NEW.quantity_delivered, v_available, NEW.production_order_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_deliveries_validate ON indicolors.order_deliveries;
CREATE TRIGGER trg_deliveries_validate
    BEFORE INSERT ON indicolors.order_deliveries
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_validate_delivery();

COMMENT ON FUNCTION indicolors.fn_validate_delivery() IS 'Calcula available_before y rechaza la entrega si quantity_delivered supera lo disponible (procesado en Estación menos ya entregado)';

CREATE OR REPLACE FUNCTION indicolors.fn_sync_ar_delivery()
RETURNS TRIGGER AS $$
DECLARE
    v_total_units INTEGER;
BEGIN
    SELECT requested_quantity INTO v_total_units
    FROM indicolors.production_orders
    WHERE production_order_id = NEW.production_order_id;

    INSERT INTO indicolors.ar_summary (
        company_id, production_order_id, client_id,
        total_units, delivered_units, pending_units,
        total_owed, total_paid, total_remaining,
        status, last_delivery_at, updated_at
    )
    VALUES (
        NEW.company_id, NEW.production_order_id, NEW.client_id,
        COALESCE(v_total_units, NEW.quantity_delivered),
        NEW.quantity_delivered,
        GREATEST(COALESCE(v_total_units, NEW.quantity_delivered) - NEW.quantity_delivered, 0),
        NEW.total_value, 0, NEW.total_value,
        'pendiente', NEW.delivered_at, now()
    )
    ON CONFLICT (production_order_id) DO UPDATE SET
        delivered_units  = indicolors.ar_summary.delivered_units + NEW.quantity_delivered,
        pending_units    = GREATEST(
                                indicolors.ar_summary.total_units
                                - (indicolors.ar_summary.delivered_units + NEW.quantity_delivered),
                                0
                            ),
        total_owed       = indicolors.ar_summary.total_owed + NEW.total_value,
        total_remaining  = (indicolors.ar_summary.total_owed + NEW.total_value)
                            - indicolors.ar_summary.total_paid,
        last_delivery_at = NEW.delivered_at,
        updated_at       = now(),
        status = CASE
            WHEN indicolors.ar_summary.total_paid
                 >= (indicolors.ar_summary.total_owed + NEW.total_value)
                 AND (indicolors.ar_summary.total_owed + NEW.total_value) > 0 THEN 'pagado'
            WHEN indicolors.ar_summary.total_paid > 0 THEN 'parcial'
            ELSE 'pendiente'
        END;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_deliveries_sync_ar ON indicolors.order_deliveries;
CREATE TRIGGER trg_deliveries_sync_ar
    AFTER INSERT ON indicolors.order_deliveries
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_sync_ar_delivery();

COMMENT ON FUNCTION indicolors.fn_sync_ar_delivery() IS 'Upsert de ar_summary tras cada entrega: actualiza unidades entregadas/faltantes y el valor adeudado, en la misma transacción del INSERT';

CREATE OR REPLACE FUNCTION indicolors.fn_sync_ar_payment()
RETURNS TRIGGER AS $$
DECLARE
    v_signed_amount NUMERIC(14,2);
BEGIN
    v_signed_amount := CASE WHEN NEW.payment_type = 'reversion' THEN -NEW.amount ELSE NEW.amount END;

    INSERT INTO indicolors.ar_summary (
        company_id, production_order_id, client_id,
        total_units, delivered_units, pending_units,
        total_owed, total_paid, total_remaining,
        status, last_payment_at, updated_at
    )
    VALUES (
        NEW.company_id, NEW.production_order_id, NEW.client_id,
        0, 0, 0, 0, v_signed_amount, -v_signed_amount,
        'pendiente', NEW.paid_at, now()
    )
    ON CONFLICT (production_order_id) DO UPDATE SET
        total_paid      = indicolors.ar_summary.total_paid + v_signed_amount,
        total_remaining = indicolors.ar_summary.total_owed
                            - (indicolors.ar_summary.total_paid + v_signed_amount),
        last_payment_at = NEW.paid_at,
        updated_at      = now(),
        status = CASE
            WHEN indicolors.ar_summary.total_owed > 0
                 AND (indicolors.ar_summary.total_paid + v_signed_amount)
                     >= indicolors.ar_summary.total_owed THEN 'pagado'
            WHEN (indicolors.ar_summary.total_paid + v_signed_amount) > 0 THEN 'parcial'
            ELSE 'pendiente'
        END;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_payments_sync_ar ON indicolors.order_payments;
CREATE TRIGGER trg_payments_sync_ar
    AFTER INSERT ON indicolors.order_payments
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_sync_ar_payment();

COMMENT ON FUNCTION indicolors.fn_sync_ar_payment() IS 'Upsert de ar_summary tras cada abono/reversión: actualiza total_paid, total_remaining y status, en la misma transacción del INSERT';
