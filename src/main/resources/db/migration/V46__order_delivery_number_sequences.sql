-- Consecutivo atómico ENT por empresa (delivery_number = ENT-{n}).
-- Serie propia: distinta de customer_orders.odp_number (ODP) y de cxc_number (CXC).

CREATE TABLE IF NOT EXISTS indicolors.order_delivery_number_sequences (
    company_id  CHARACTER VARYING(64) NOT NULL,
    last_value  BIGINT                NOT NULL DEFAULT 0,
    CONSTRAINT order_delivery_number_sequences_pkey PRIMARY KEY (company_id),
    CONSTRAINT order_delivery_number_sequences_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT order_delivery_number_sequences_last_value_check CHECK (last_value >= 0)
);

COMMENT ON TABLE indicolors.order_delivery_number_sequences IS
    'Consecutivo ENT-{n} por compañía para delivery_number (entregas / reversiones). '
    'Serie propia, distinta de customer_orders_number_sequences (ODP de pedidos) '
    'y de accounts_receivable_number_sequences (CXC).';
COMMENT ON COLUMN indicolors.order_delivery_number_sequences.company_id IS
    'Identificador de la empresa dueña del contador';
COMMENT ON COLUMN indicolors.order_delivery_number_sequences.last_value IS
    'Último número asignado (delivery_number = ENT-{last_value})';

GRANT ALL PRIVILEGES ON TABLE indicolors.order_delivery_number_sequences TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.order_delivery_number_sequences TO indicolors_app;
