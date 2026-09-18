-- Agregado derivado de cuentas por cobrar por OP (solo escrito por triggers).
-- Incluye vencimiento (due_date) para aging/alertas; listo para que FE fije due_date después.

CREATE TABLE IF NOT EXISTS indicolors.accounts_receivable (
    accounts_receivable_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id             CHARACTER VARYING(64)       NOT NULL,
    cxc_number             CHARACTER VARYING(32)       NOT NULL,
    production_order_id    CHARACTER VARYING(64)       NOT NULL,
    client_id              CHARACTER VARYING(64)       NOT NULL,

    total_units            INTEGER                     NOT NULL DEFAULT 0,
    delivered_units        INTEGER                     NOT NULL DEFAULT 0,
    pending_units          INTEGER                     NOT NULL DEFAULT 0,

    total_owed             NUMERIC(14,2)               NOT NULL DEFAULT 0,
    total_paid             NUMERIC(14,2)               NOT NULL DEFAULT 0,
    total_remaining        NUMERIC(14,2)               NOT NULL DEFAULT 0,

    total_cash_paid        NUMERIC(14,2)               NOT NULL DEFAULT 0,
    total_withheld         NUMERIC(14,2)               NOT NULL DEFAULT 0,
    total_advance_paid     NUMERIC(14,2)               NOT NULL DEFAULT 0,

    opened_at              TIMESTAMP WITHOUT TIME ZONE,
    due_date               DATE,
    payment_term_days      INTEGER                     NOT NULL DEFAULT 0,

    status                 CHARACTER VARYING(16)       NOT NULL DEFAULT 'pendiente',
    last_delivery_at       TIMESTAMP WITHOUT TIME ZONE,
    last_payment_number    CHARACTER VARYING(32),
    last_payment_at        TIMESTAMP WITHOUT TIME ZONE,

    updated_at             TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT accounts_receivable_pkey PRIMARY KEY (accounts_receivable_id),
    CONSTRAINT accounts_receivable_production_order_unique UNIQUE (production_order_id),
    CONSTRAINT accounts_receivable_cxc_number_company_unique UNIQUE (company_id, cxc_number),
    CONSTRAINT accounts_receivable_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT accounts_receivable_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT accounts_receivable_client_fk
        FOREIGN KEY (client_id) REFERENCES indicolors.clients (client_id),
    CONSTRAINT accounts_receivable_status_check
        CHECK (status IN ('pendiente', 'parcial', 'pagado', 'anulado')),
    CONSTRAINT accounts_receivable_units_check
        CHECK (total_units >= 0 AND delivered_units >= 0 AND pending_units >= 0),
    CONSTRAINT accounts_receivable_payment_term_days_check
        CHECK (payment_term_days >= 0),
    CONSTRAINT accounts_receivable_cxc_number_format_check
        CHECK (cxc_number ~ '^CXC-[0-9]+$')
);

CREATE INDEX IF NOT EXISTS idx_accounts_receivable_company ON indicolors.accounts_receivable (company_id, status);
CREATE INDEX IF NOT EXISTS idx_accounts_receivable_client ON indicolors.accounts_receivable (company_id, client_id);
CREATE INDEX IF NOT EXISTS idx_accounts_receivable_cxc_number ON indicolors.accounts_receivable (company_id, cxc_number);
CREATE INDEX IF NOT EXISTS idx_accounts_receivable_due_date
    ON indicolors.accounts_receivable (company_id, due_date)
    WHERE total_remaining > 0 AND due_date IS NOT NULL;

COMMENT ON TABLE indicolors.accounts_receivable IS
    'Agregado por OP para CxC: saldos, desglose de liquidación y vencimiento. Solo triggers; nunca edición manual desde backend';

COMMENT ON COLUMN indicolors.accounts_receivable.accounts_receivable_id IS 'Identificador único (UUID) de la Cuenta por cobrar';
COMMENT ON COLUMN indicolors.accounts_receivable.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.accounts_receivable.cxc_number IS 'Consecutivo CXC-{n}; lo asigna el trigger en el primer INSERT';
COMMENT ON COLUMN indicolors.accounts_receivable.production_order_id IS 'OP asociada (1:1)';
COMMENT ON COLUMN indicolors.accounts_receivable.client_id IS 'Cliente de la OP';
COMMENT ON COLUMN indicolors.accounts_receivable.total_units IS 'Unidades totales de la OP (snapshot requested_quantity)';
COMMENT ON COLUMN indicolors.accounts_receivable.delivered_units IS 'Unidades entregadas netas';
COMMENT ON COLUMN indicolors.accounts_receivable.pending_units IS 'total_units - delivered_units, nunca negativo';
COMMENT ON COLUMN indicolors.accounts_receivable.total_owed IS 'Valor acumulado de lo entregado';
COMMENT ON COLUMN indicolors.accounts_receivable.total_paid IS 'Suma neta de liquidaciones (abono+anticipo+retencion - reversiones)';
COMMENT ON COLUMN indicolors.accounts_receivable.total_remaining IS 'total_owed - total_paid';
COMMENT ON COLUMN indicolors.accounts_receivable.total_cash_paid IS 'Suma neta de abonos en caja (abono - reversiones de abono)';
COMMENT ON COLUMN indicolors.accounts_receivable.total_withheld IS 'Suma neta de retenciones sufridas';
COMMENT ON COLUMN indicolors.accounts_receivable.total_advance_paid IS 'Suma neta de anticipos aplicados/registrados';
COMMENT ON COLUMN indicolors.accounts_receivable.opened_at IS
    'Fecha/hora de la primera entrega que abrió la CxC (delivered_at). Se fija una sola vez; no se actualiza con entregas posteriores ni se limpia al revertir (valor histórico).';
COMMENT ON COLUMN indicolors.accounts_receivable.due_date IS
    'Fecha límite de pago (opened_at::date + payment_term_days). FE podrá actualizarla al emitir factura';
COMMENT ON COLUMN indicolors.accounts_receivable.payment_term_days IS
    'Días de crédito snapshot desde clients.credit_days al abrir la CxC';
COMMENT ON COLUMN indicolors.accounts_receivable.status IS
    'pendiente|parcial|pagado|anulado (estado de liquidación; el aging se calcula aparte con due_date)';
COMMENT ON COLUMN indicolors.accounts_receivable.last_delivery_at IS 'delivered_at de la última entrega';
COMMENT ON COLUMN indicolors.accounts_receivable.last_payment_number IS
    'Último payment_number (ABN-{n}) vigente de la OP. Null si no hay liquidaciones netas. No es el id del agregado (ese es cxc_number).';
COMMENT ON COLUMN indicolors.accounts_receivable.last_payment_at IS
    'paid_at del último abono/anticipo/retención vigente (no reversión). Null si no hay liquidaciones netas.';
COMMENT ON COLUMN indicolors.accounts_receivable.updated_at IS 'Última actualización del agregado';

GRANT ALL PRIVILEGES ON TABLE indicolors.accounts_receivable TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.accounts_receivable TO indicolors_app;
