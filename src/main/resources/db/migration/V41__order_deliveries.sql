-- Entregas comerciales append-only (mÃ³dulo Pedidos) + reversiones.

CREATE TABLE IF NOT EXISTS indicolors.order_deliveries (
    order_delivery_id     CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id            CHARACTER VARYING(64)       NOT NULL,
    delivery_number       CHARACTER VARYING(32)       NOT NULL,
    production_order_id   CHARACTER VARYING(64)       NOT NULL,
    client_id             CHARACTER VARYING(64)       NOT NULL,
    seller_id             CHARACTER VARYING(64),

    movement_type         CHARACTER VARYING(16)       NOT NULL DEFAULT 'entrega',
    delivery_type         CHARACTER VARYING(16)       NOT NULL,
    reversed_delivery_id  CHARACTER VARYING(64),
    quantity_delivered    INTEGER                     NOT NULL,
    unit_price            NUMERIC(12,2)               NOT NULL DEFAULT 0,
    total_value           NUMERIC(14,2)               NOT NULL DEFAULT 0,
    available_before      INTEGER                     NOT NULL DEFAULT 0,

    work_name_snapshot    CHARACTER VARYING(150),
    client_name_snapshot  CHARACTER VARYING(200),

    delivered_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    delivered_by          CHARACTER VARYING(64)       NOT NULL,
    notes                 TEXT,

    created_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT order_deliveries_pkey PRIMARY KEY (order_delivery_id),
    CONSTRAINT order_deliveries_delivery_number_company_unique UNIQUE (company_id, delivery_number),
    CONSTRAINT order_deliveries_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT order_deliveries_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id),
    CONSTRAINT order_deliveries_client_fk
        FOREIGN KEY (client_id) REFERENCES indicolors.clients (client_id),
    CONSTRAINT order_deliveries_seller_fk
        FOREIGN KEY (seller_id) REFERENCES indicolors.sellers (seller_id),
    CONSTRAINT order_deliveries_delivered_by_fk
        FOREIGN KEY (delivered_by) REFERENCES indicolors.users (user_id),
    CONSTRAINT order_deliveries_reversed_fk
        FOREIGN KEY (reversed_delivery_id) REFERENCES indicolors.order_deliveries (order_delivery_id),
    CONSTRAINT order_deliveries_type_check
        CHECK (delivery_type IN ('parcial', 'total')),
    CONSTRAINT order_deliveries_movement_type_check
        CHECK (movement_type IN ('entrega', 'reversion')),
    CONSTRAINT order_deliveries_reversion_ref_check
        CHECK (movement_type <> 'reversion' OR reversed_delivery_id IS NOT NULL),
    CONSTRAINT order_deliveries_entrega_no_ref_check
        CHECK (movement_type <> 'entrega' OR reversed_delivery_id IS NULL),
    CONSTRAINT order_deliveries_quantity_check
        CHECK (quantity_delivered > 0),
    CONSTRAINT order_deliveries_unit_price_check
        CHECK (unit_price >= 0),
    CONSTRAINT order_deliveries_available_before_check
        CHECK (available_before >= 0),
    CONSTRAINT order_deliveries_delivery_number_format_check
        CHECK (delivery_number ~ '^ENT-[0-9]+$')
);

CREATE INDEX IF NOT EXISTS idx_order_deliveries_company_id ON indicolors.order_deliveries (company_id);
CREATE INDEX IF NOT EXISTS idx_order_deliveries_order_time ON indicolors.order_deliveries (company_id, production_order_id, delivered_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_deliveries_client_time ON indicolors.order_deliveries (company_id, client_id, delivered_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_deliveries_seller_time ON indicolors.order_deliveries (company_id, seller_id, delivered_at DESC)
    WHERE seller_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_order_deliveries_delivery_number ON indicolors.order_deliveries (company_id, delivery_number);
CREATE UNIQUE INDEX IF NOT EXISTS uq_order_deliveries_reversed_once
    ON indicolors.order_deliveries (company_id, reversed_delivery_id)
    WHERE reversed_delivery_id IS NOT NULL;

COMMENT ON TABLE indicolors.order_deliveries IS 'BitÃ¡cora append-only de entregas comerciales (parciales/totales) y sus reversiones (movement_type=reversion) de una OP a su cliente/representante. No admite UPDATE ni DELETE en producciÃ³n; correcciones = nueva fila (reversiÃ³n) con notes explicativa';

COMMENT ON COLUMN indicolors.order_deliveries.order_delivery_id IS 'Identificador Ãºnico de la entrega o de su reversiÃ³n';
COMMENT ON COLUMN indicolors.order_deliveries.company_id IS 'Identificador de la empresa dueÃ±a del registro';
COMMENT ON COLUMN indicolors.order_deliveries.delivery_number IS
    'Consecutivo de entrega por compaÃ±Ã­a (ENT-{n}), generado por el backend; '
    'serie propia distinta de customer_orders.odp_number (ODP) y de cxc_number (CXC). '
    'Ãšnico por compaÃ±Ã­a; cada reversiÃ³n tambiÃ©n recibe el suyo.';
COMMENT ON COLUMN indicolors.order_deliveries.production_order_id IS 'Identificador de la Orden de ProducciÃ³n entregada';
COMMENT ON COLUMN indicolors.order_deliveries.client_id IS 'Cliente que recibe la entrega (snapshot desde production_orders.client_id)';
COMMENT ON COLUMN indicolors.order_deliveries.seller_id IS 'Representante/vendedor asociado a la entrega, si aplica';
COMMENT ON COLUMN indicolors.order_deliveries.movement_type IS 'entrega=movimiento normal que suma disponibilidad entregada | reversion=anulaciÃ³n de una entrega previa que la resta (append-only, nunca UPDATE/DELETE)';
COMMENT ON COLUMN indicolors.order_deliveries.delivery_type IS 'parcial=entrega parcial de unidades | total=entrega final que cierra la OP comercialmente; en una reversiÃ³n describe el subtipo de la entrega original que se anula';
COMMENT ON COLUMN indicolors.order_deliveries.reversed_delivery_id IS 'order_delivery_id de la entrega original que se anula; obligatorio si movement_type=reversion, y solo puede apuntar a una fila con movement_type=entrega que aÃºn no haya sido revertida';
COMMENT ON COLUMN indicolors.order_deliveries.quantity_delivered IS 'Unidades del movimiento (> 0); en una reversiÃ³n es la misma cantidad de la entrega original que se estÃ¡ anulando, el trigger la resta';
COMMENT ON COLUMN indicolors.order_deliveries.unit_price IS 'Precio unitario snapshot usado para valorizar esta entrega';
COMMENT ON COLUMN indicolors.order_deliveries.total_value IS 'quantity_delivered * unit_price, calculado por el backend al insertar; en una reversiÃ³n debe igualar el total_value de la entrega original';
COMMENT ON COLUMN indicolors.order_deliveries.available_before IS 'Unidades disponibles para entrega justo antes de este movimiento; lo calcula el trigger de validaciÃ³n, no lo envÃ­a el cliente';
COMMENT ON COLUMN indicolors.order_deliveries.work_name_snapshot IS 'Snapshot de production_orders.work_name al momento de insertar';
COMMENT ON COLUMN indicolors.order_deliveries.client_name_snapshot IS 'Snapshot de clients.name al momento de insertar';
COMMENT ON COLUMN indicolors.order_deliveries.delivered_at IS 'Fecha/hora real de la entrega (puede venir del cliente)';
COMMENT ON COLUMN indicolors.order_deliveries.delivered_by IS 'Usuario que registrÃ³ la entrega o la reversiÃ³n';
COMMENT ON COLUMN indicolors.order_deliveries.notes IS 'Nota libre; usada tambiÃ©n para explicar anulaciones';
COMMENT ON COLUMN indicolors.order_deliveries.created_at IS 'Fecha y hora de persistencia del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.order_deliveries TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.order_deliveries TO indicolors_app;


CREATE OR REPLACE FUNCTION indicolors.fn_validate_delivery()
RETURNS TRIGGER AS $$
DECLARE
    v_processed     INTEGER;
    v_delivered     INTEGER;
    v_available     INTEGER;
    v_orig          indicolors.order_deliveries%ROWTYPE;
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
            RAISE EXCEPTION 'Solo se puede anular una entrega original, no otra reversiÃ³n (%)', NEW.reversed_delivery_id;
        END IF;

        IF v_orig.production_order_id <> NEW.production_order_id
           OR v_orig.company_id <> NEW.company_id THEN
            RAISE EXCEPTION 'La reversiÃ³n debe pertenecer a la misma OP y compaÃ±Ã­a que la entrega original %', NEW.reversed_delivery_id;
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
                'La reversiÃ³n debe anular exactamente la entrega original: % unidades por %',
                v_orig.quantity_delivered, v_orig.total_value;
        END IF;

        -- Abonos se aplican sobre amountToCollect de la OP (API), no sobre valor entregado:
        -- no se valida total_paid vs total_owed de entregas al anular.

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
    'entrega: calcula available_before y rechaza si quantity_delivered supera lo disponible. reversion: valida entrega original (Abonos usa amountToCollect OP, no bloquea por total_paid vs valor entregado)';

-- ---------------------------------------------------------------------------
-- Sync entrega: asigna CXC si la cuenta shell aÃºn no tiene; no regenera ABN.
-- ---------------------------------------------------------------------------
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
            cxc_number, abonos_number, company_id, production_order_id, client_id,
            total_units, delivered_units, pending_units,
            total_owed, total_paid, total_remaining,
            total_cash_paid, total_withheld, total_advance_paid,
            opened_at, due_date, payment_term_days,
            status, last_delivery_at, updated_at
        )
        VALUES (
            indicolors.fn_next_cxc_number(NEW.company_id),
            indicolors.fn_next_abonos_number(NEW.company_id),
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
            cxc_number = CASE
                WHEN cxc_number IS NULL AND NEW.movement_type = 'entrega'
                    THEN indicolors.fn_next_cxc_number(NEW.company_id)
                ELSE cxc_number
            END,
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
    'Upsert CxC tras entrega/reversiÃ³n; asigna cxc_number en apertura si la cuenta ABN ya existÃ­a sin CXC; '
    'opened_at/due_date solo en apertura; nunca regenera abonos_number';
