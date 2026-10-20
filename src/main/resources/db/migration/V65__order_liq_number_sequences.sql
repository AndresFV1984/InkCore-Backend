-- Consecutivo LIQ-{n} por compañía (payment_number de liquidaciones).
-- Serie separada de ABN-{n} (abonos_number / cuenta de Abonos).

CREATE TABLE IF NOT EXISTS indicolors.order_liq_number_sequences (
    company_id  CHARACTER VARYING(64) NOT NULL,
    last_value  BIGINT                NOT NULL DEFAULT 0,
    CONSTRAINT order_liq_number_sequences_pkey PRIMARY KEY (company_id),
    CONSTRAINT order_liq_number_sequences_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT order_liq_number_sequences_last_value_check CHECK (last_value >= 0)
);

COMMENT ON TABLE indicolors.order_liq_number_sequences IS
    'Consecutivo LIQ-{n} por compañía para payment_number (liquidaciones / movimientos). '
    'Serie propia, distinta de order_payment_number_sequences (ABN de cuentas).';
COMMENT ON COLUMN indicolors.order_liq_number_sequences.company_id IS
    'Identificador de la empresa dueña del contador';
COMMENT ON COLUMN indicolors.order_liq_number_sequences.last_value IS
    'Último LIQ asignado (payment_number = LIQ-{last_value})';

GRANT ALL PRIVILEGES ON TABLE indicolors.order_liq_number_sequences TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.order_liq_number_sequences TO indicolors_app;
