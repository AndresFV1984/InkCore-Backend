-- Consecutivo atómico ODP por empresa (customer_orders.odp_number = ODP-{n}).

CREATE TABLE IF NOT EXISTS indicolors.customer_orders_number_sequences (
    company_id  CHARACTER VARYING(64) NOT NULL,
    last_value  BIGINT                NOT NULL DEFAULT 0,
    CONSTRAINT customer_orders_number_sequences_pkey PRIMARY KEY (company_id),
    CONSTRAINT customer_orders_number_sequences_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT customer_orders_number_sequences_last_value_check CHECK (last_value >= 0)
);

COMMENT ON TABLE indicolors.customer_orders_number_sequences IS
    'Último consecutivo de odp_number emitido por compañía; se incrementa al crear un pedido comercial (customer_orders)';
COMMENT ON COLUMN indicolors.customer_orders_number_sequences.company_id IS
    'Identificador de la empresa dueña del contador';
COMMENT ON COLUMN indicolors.customer_orders_number_sequences.last_value IS
    'Último número asignado (el odp_number expuesto es ODP-{last_value})';

GRANT ALL PRIVILEGES ON TABLE indicolors.customer_orders_number_sequences TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.customer_orders_number_sequences TO indicolors_app;
