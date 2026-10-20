-- Consecutivo atómico ABN por empresa (abonos_number = ABN-{n}).
-- Serie propia de cuentas; distinta de order_liq_number_sequences (LIQ de movimientos).

CREATE TABLE IF NOT EXISTS indicolors.order_payment_number_sequences (
    company_id  CHARACTER VARYING(64) NOT NULL,
    last_value  BIGINT                NOT NULL DEFAULT 0,
    CONSTRAINT order_payment_number_sequences_pkey PRIMARY KEY (company_id),
    CONSTRAINT order_payment_number_sequences_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT order_payment_number_sequences_last_value_check CHECK (last_value >= 0)
);

COMMENT ON TABLE indicolors.order_payment_number_sequences IS
    'Consecutivo ABN-{n} por compañía para abonos_number (id del agregado de Abonos). '
    'Serie propia; distinta de order_liq_number_sequences (LIQ de movimientos).';
COMMENT ON COLUMN indicolors.order_payment_number_sequences.company_id IS
    'Identificador de la empresa dueña del contador';
COMMENT ON COLUMN indicolors.order_payment_number_sequences.last_value IS
    'Último ABN asignado (abonos_number = ABN-{last_value})';

GRANT ALL PRIVILEGES ON TABLE indicolors.order_payment_number_sequences TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.order_payment_number_sequences TO indicolors_app;
