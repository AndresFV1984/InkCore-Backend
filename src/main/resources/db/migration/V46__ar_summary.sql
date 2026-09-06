-- Agregado derivado de cuentas por cobrar por OP (solo escrito por triggers).

CREATE TABLE IF NOT EXISTS indicolors.ar_summary (
    company_id           CHARACTER VARYING(64)       NOT NULL,
    production_order_id  CHARACTER VARYING(64)       NOT NULL,
    client_id            CHARACTER VARYING(64)       NOT NULL,

    total_units          INTEGER                     NOT NULL DEFAULT 0,
    delivered_units      INTEGER                     NOT NULL DEFAULT 0,
    pending_units        INTEGER                     NOT NULL DEFAULT 0,

    total_owed           NUMERIC(14,2)                NOT NULL DEFAULT 0,
    total_paid           NUMERIC(14,2)                NOT NULL DEFAULT 0,
    total_remaining      NUMERIC(14,2)                NOT NULL DEFAULT 0,

    status               CHARACTER VARYING(16)        NOT NULL DEFAULT 'pendiente',
    last_delivery_at     TIMESTAMP WITHOUT TIME ZONE,
    last_payment_at      TIMESTAMP WITHOUT TIME ZONE,

    updated_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT ar_summary_pkey PRIMARY KEY (production_order_id),
    CONSTRAINT ar_summary_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT ar_summary_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT ar_summary_client_fk
        FOREIGN KEY (client_id) REFERENCES indicolors.clients (client_id),
    CONSTRAINT ar_summary_status_check
        CHECK (status IN ('pendiente', 'parcial', 'pagado')),
    CONSTRAINT ar_summary_units_check
        CHECK (total_units >= 0 AND delivered_units >= 0 AND pending_units >= 0)
);

CREATE INDEX IF NOT EXISTS idx_ar_summary_company ON indicolors.ar_summary (company_id, status);
CREATE INDEX IF NOT EXISTS idx_ar_summary_client ON indicolors.ar_summary (company_id, client_id);

COMMENT ON TABLE indicolors.ar_summary IS 'Agregado por OP para el dashboard Cuentas por cobrar: unidades entregadas/faltantes, total abonado y saldo restante. Se recalcula por trigger en la misma transacción de order_deliveries/order_payments; nunca se edita manualmente desde el backend';

COMMENT ON COLUMN indicolors.ar_summary.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.ar_summary.production_order_id IS 'OP a la que pertenece el resumen (PK, una fila por orden)';
COMMENT ON COLUMN indicolors.ar_summary.client_id IS 'Cliente de la OP';
COMMENT ON COLUMN indicolors.ar_summary.total_units IS 'Unidades totales de la OP (snapshot de production_orders.requested_quantity)';
COMMENT ON COLUMN indicolors.ar_summary.delivered_units IS 'Unidades entregadas acumuladas (suma de order_deliveries.quantity_delivered)';
COMMENT ON COLUMN indicolors.ar_summary.pending_units IS 'total_units - delivered_units, nunca negativo';
COMMENT ON COLUMN indicolors.ar_summary.total_owed IS 'Valor acumulado de lo entregado (suma de order_deliveries.total_value)';
COMMENT ON COLUMN indicolors.ar_summary.total_paid IS 'Suma neta de abonos (abonos - reversiones)';
COMMENT ON COLUMN indicolors.ar_summary.total_remaining IS 'total_owed - total_paid';
COMMENT ON COLUMN indicolors.ar_summary.status IS 'pendiente=sin abonos | parcial=abonos parciales | pagado=total_paid >= total_owed';
COMMENT ON COLUMN indicolors.ar_summary.last_delivery_at IS 'delivered_at de la última entrega registrada';
COMMENT ON COLUMN indicolors.ar_summary.last_payment_at IS 'paid_at del último abono registrado';
COMMENT ON COLUMN indicolors.ar_summary.updated_at IS 'Fecha y hora de la última actualización del agregado';

GRANT ALL PRIVILEGES ON TABLE indicolors.ar_summary TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.ar_summary TO indicolors_app;
