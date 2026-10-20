-- Consecutivo ADN-{n} por compañía para adicionales negociados de una OP.

CREATE TABLE IF NOT EXISTS indicolors.order_negotiated_addition_number_sequences (
    company_id  CHARACTER VARYING(64) NOT NULL,
    last_value  BIGINT                NOT NULL DEFAULT 0,
    CONSTRAINT order_negotiated_addition_number_sequences_pkey PRIMARY KEY (company_id),
    CONSTRAINT order_negotiated_addition_number_sequences_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT order_negotiated_addition_number_sequences_last_value_check CHECK (last_value >= 0)
);

COMMENT ON TABLE indicolors.order_negotiated_addition_number_sequences IS
    'Consecutivo ADN-{n} por compañía para adicionales negociados de una OP.';
COMMENT ON COLUMN indicolors.order_negotiated_addition_number_sequences.company_id IS
    'Identificador de la empresa dueña del contador';
COMMENT ON COLUMN indicolors.order_negotiated_addition_number_sequences.last_value IS
    'Último ADN asignado (addition_number = ADN-{last_value})';

GRANT ALL PRIVILEGES ON TABLE indicolors.order_negotiated_addition_number_sequences TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.order_negotiated_addition_number_sequences TO indicolors_app;
