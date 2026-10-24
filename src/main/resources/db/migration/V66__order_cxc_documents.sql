-- Documentos CxC (cartera): en InkCore "factura" = CxC.
-- Una CxC por entrega (ENT) y, más adelante, una por ADN facturado.
-- La fila accounts_receivable (1:1 OP) sigue como shell ABN / agregados legacy.

CREATE TABLE IF NOT EXISTS indicolors.order_cxc_documents (
    cxc_document_id       CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id            CHARACTER VARYING(64)       NOT NULL,
    cxc_number            CHARACTER VARYING(32)       NOT NULL,
    production_order_id   CHARACTER VARYING(64)       NOT NULL,
    customer_order_id     CHARACTER VARYING(64),
    client_id             CHARACTER VARYING(64)       NOT NULL,

    concept               CHARACTER VARYING(16)       NOT NULL,
    source_delivery_id    CHARACTER VARYING(64),
    source_addition_id    CHARACTER VARYING(64),

    total                 NUMERIC(14,2)               NOT NULL,
    status                CHARACTER VARYING(16)       NOT NULL DEFAULT 'pendiente',

    issued_at             TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    due_date              DATE,
    payment_term_days     INTEGER                     NOT NULL DEFAULT 0,

    created_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT order_cxc_documents_pkey PRIMARY KEY (cxc_document_id),
    CONSTRAINT order_cxc_documents_number_company_unique UNIQUE (company_id, cxc_number),
    CONSTRAINT order_cxc_documents_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT order_cxc_documents_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id),
    CONSTRAINT order_cxc_documents_customer_order_fk
        FOREIGN KEY (customer_order_id) REFERENCES indicolors.customer_orders (customer_order_id),
    CONSTRAINT order_cxc_documents_client_fk
        FOREIGN KEY (client_id) REFERENCES indicolors.clients (client_id),
    CONSTRAINT order_cxc_documents_delivery_fk
        FOREIGN KEY (source_delivery_id) REFERENCES indicolors.order_deliveries (order_delivery_id),
    CONSTRAINT order_cxc_documents_addition_fk
        FOREIGN KEY (source_addition_id)
            REFERENCES indicolors.order_negotiated_additions (order_negotiated_addition_id),
    CONSTRAINT order_cxc_documents_concept_check
        CHECK (concept IN ('entrega', 'extra')),
    CONSTRAINT order_cxc_documents_status_check
        CHECK (status IN ('pendiente', 'parcial', 'pagada', 'anulada')),
    CONSTRAINT order_cxc_documents_total_check
        CHECK (total > 0),
    CONSTRAINT order_cxc_documents_payment_term_days_check
        CHECK (payment_term_days >= 0),
    CONSTRAINT order_cxc_documents_cxc_number_format_check
        CHECK (cxc_number ~ '^CXC-[0-9]+$'),
    CONSTRAINT order_cxc_documents_entrega_source_check
        CHECK (concept <> 'entrega' OR source_delivery_id IS NOT NULL),
    CONSTRAINT order_cxc_documents_extra_source_check
        CHECK (concept <> 'extra' OR source_addition_id IS NOT NULL),
    CONSTRAINT order_cxc_documents_entrega_no_addition_check
        CHECK (concept <> 'entrega' OR source_addition_id IS NULL),
    CONSTRAINT order_cxc_documents_extra_no_delivery_check
        CHECK (concept <> 'extra' OR source_delivery_id IS NULL)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_order_cxc_documents_delivery
    ON indicolors.order_cxc_documents (company_id, source_delivery_id)
    WHERE source_delivery_id IS NOT NULL AND status <> 'anulada';

CREATE UNIQUE INDEX IF NOT EXISTS uq_order_cxc_documents_addition
    ON indicolors.order_cxc_documents (company_id, source_addition_id)
    WHERE source_addition_id IS NOT NULL AND status <> 'anulada';

CREATE INDEX IF NOT EXISTS idx_order_cxc_documents_company_id
    ON indicolors.order_cxc_documents (company_id);
CREATE INDEX IF NOT EXISTS idx_order_cxc_documents_company_status
    ON indicolors.order_cxc_documents (company_id, status);
CREATE INDEX IF NOT EXISTS idx_order_cxc_documents_order
    ON indicolors.order_cxc_documents (company_id, production_order_id, issued_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_cxc_documents_client
    ON indicolors.order_cxc_documents (company_id, client_id, issued_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_cxc_documents_cxc_number
    ON indicolors.order_cxc_documents (company_id, cxc_number);
CREATE INDEX IF NOT EXISTS idx_order_cxc_documents_due_date
    ON indicolors.order_cxc_documents (company_id, due_date)
    WHERE status IN ('pendiente', 'parcial') AND due_date IS NOT NULL;

COMMENT ON TABLE indicolors.order_cxc_documents IS
    'Documento de cartera CxC (en producto = factura). 1 ENT → 1 CxC entrega; ADN facturado → CxC extra. '
    'Serie CXC-{n} vía fn_next_cxc_number. Distinto del shell accounts_receivable (ABN 1:1 OP).';

COMMENT ON COLUMN indicolors.order_cxc_documents.cxc_document_id IS 'Identificador único del documento CxC';
COMMENT ON COLUMN indicolors.order_cxc_documents.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.order_cxc_documents.cxc_number IS
    'Número visible CXC-{n}. Misma serie que el legado accounts_receivable.cxc_number.';
COMMENT ON COLUMN indicolors.order_cxc_documents.production_order_id IS 'Orden de Producción a la que pertenece el documento';
COMMENT ON COLUMN indicolors.order_cxc_documents.customer_order_id IS 'Pedido comercial (customer_orders) asociado, si aplica';
COMMENT ON COLUMN indicolors.order_cxc_documents.client_id IS 'Cliente snapshot desde production_orders.client_id';
COMMENT ON COLUMN indicolors.order_cxc_documents.concept IS
    'entrega = valor de entrega ENT; extra = adicional negociado ADN del cobro OP.';
COMMENT ON COLUMN indicolors.order_cxc_documents.source_delivery_id IS
    'order_delivery_id de la ENT que originó la CxC; obligatorio si concept=entrega';
COMMENT ON COLUMN indicolors.order_cxc_documents.source_addition_id IS
    'order_negotiated_addition_id del ADN facturado; obligatorio si concept=extra';
COMMENT ON COLUMN indicolors.order_cxc_documents.total IS
    'Total del documento; no se reduce por pagos. El saldo sale de aplicaciones.';
COMMENT ON COLUMN indicolors.order_cxc_documents.status IS
    'pendiente | parcial | pagada | anulada';
COMMENT ON COLUMN indicolors.order_cxc_documents.issued_at IS 'Fecha/hora de emisión del documento CxC';
COMMENT ON COLUMN indicolors.order_cxc_documents.due_date IS
    'Fecha de vencimiento (Net N desde issued_at según payment_term_days); NULL si contado';
COMMENT ON COLUMN indicolors.order_cxc_documents.payment_term_days IS
    'Días de crédito (Net N) al emitir; 0 = contado';
COMMENT ON COLUMN indicolors.order_cxc_documents.created_at IS 'Fecha y hora de persistencia del registro';
COMMENT ON COLUMN indicolors.order_cxc_documents.updated_at IS 'Última actualización del documento (p. ej. status)';

GRANT ALL PRIVILEGES ON TABLE indicolors.order_cxc_documents TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.order_cxc_documents TO indicolors_app;
