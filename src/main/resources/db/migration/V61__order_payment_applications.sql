-- Aplicaciones de LIQ a CxC o saldo a favor del pedido.
-- Depende de: order_payments (V44), order_cxc_documents (V66), customer_orders.

CREATE TABLE IF NOT EXISTS indicolors.order_payment_applications (
    application_id         CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id             CHARACTER VARYING(64)       NOT NULL,
    payment_id             CHARACTER VARYING(64)       NOT NULL,
    production_order_id    CHARACTER VARYING(64)       NOT NULL,
    customer_order_id      CHARACTER VARYING(64),
    cxc_document_id        CHARACTER VARYING(64),
    amount                 NUMERIC(14,2)               NOT NULL,
    origin_application_id  CHARACTER VARYING(64),
    status                 CHARACTER VARYING(16)       NOT NULL DEFAULT 'activo',
    applied_at             TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    created_at             TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT order_payment_applications_pkey PRIMARY KEY (application_id),
    CONSTRAINT order_payment_applications_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT order_payment_applications_payment_fk
        FOREIGN KEY (payment_id) REFERENCES indicolors.order_payments (order_payment_id),
    CONSTRAINT order_payment_applications_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id),
    CONSTRAINT order_payment_applications_customer_order_fk
        FOREIGN KEY (customer_order_id) REFERENCES indicolors.customer_orders (customer_order_id),
    CONSTRAINT order_payment_applications_cxc_fk
        FOREIGN KEY (cxc_document_id) REFERENCES indicolors.order_cxc_documents (cxc_document_id),
    CONSTRAINT order_payment_applications_origin_fk
        FOREIGN KEY (origin_application_id)
            REFERENCES indicolors.order_payment_applications (application_id),
    CONSTRAINT order_payment_applications_amount_check
        CHECK (amount > 0),
    CONSTRAINT order_payment_applications_status_check
        CHECK (status IN ('activo', 'reversado')),
    CONSTRAINT order_payment_applications_consume_favor_check
        CHECK (origin_application_id IS NULL OR cxc_document_id IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_order_payment_applications_company_id
    ON indicolors.order_payment_applications (company_id);
CREATE INDEX IF NOT EXISTS idx_order_payment_applications_payment
    ON indicolors.order_payment_applications (company_id, payment_id);
CREATE INDEX IF NOT EXISTS idx_order_payment_applications_order
    ON indicolors.order_payment_applications (company_id, production_order_id, applied_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_payment_applications_cxc
    ON indicolors.order_payment_applications (company_id, cxc_document_id)
    WHERE cxc_document_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_order_payment_applications_favor
    ON indicolors.order_payment_applications (company_id, production_order_id)
    WHERE cxc_document_id IS NULL AND status = 'activo';

COMMENT ON TABLE indicolors.order_payment_applications IS
    'Aplicación de un LIQ: a una CxC (cxc_document_id) o saldo a favor del pedido (NULL). '
    'Consumo de favor → CxC usa origin_application_id. Suma apps activas del pago = payment.amount.';

COMMENT ON COLUMN indicolors.order_payment_applications.application_id IS
    'Identificador único de la aplicación de pago';
COMMENT ON COLUMN indicolors.order_payment_applications.company_id IS
    'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.order_payment_applications.payment_id IS
    'order_payment_id (LIQ) que se aplica';
COMMENT ON COLUMN indicolors.order_payment_applications.production_order_id IS
    'Orden de Producción contra la que se aplica el LIQ';
COMMENT ON COLUMN indicolors.order_payment_applications.customer_order_id IS
    'Pedido comercial (customer_orders) asociado, si aplica';
COMMENT ON COLUMN indicolors.order_payment_applications.cxc_document_id IS
    'NULL = saldo a favor del pedido (anticipo/pasivo). NOT NULL = aplicado a esa CxC.';
COMMENT ON COLUMN indicolors.order_payment_applications.amount IS
    'Monto aplicado, siempre positivo';
COMMENT ON COLUMN indicolors.order_payment_applications.origin_application_id IS
    'Si se consume saldo a favor hacia una CxC, apunta a la app de favor de origen.';
COMMENT ON COLUMN indicolors.order_payment_applications.status IS
    'activo = vigente en saldos | reversado = anulado (append-only vía nueva fila o marca)';
COMMENT ON COLUMN indicolors.order_payment_applications.applied_at IS
    'Fecha/hora en que se registró la aplicación';
COMMENT ON COLUMN indicolors.order_payment_applications.created_at IS
    'Fecha y hora de persistencia del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.order_payment_applications TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.order_payment_applications TO indicolors_app;
