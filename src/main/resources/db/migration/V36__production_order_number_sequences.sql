-- Contador atómico de order_number por empresa (consecutivo corto: OP-1, OP-2, ...).

CREATE TABLE IF NOT EXISTS indicolors.production_order_number_sequences (
    company_id  CHARACTER VARYING(64) NOT NULL,
    last_value  BIGINT                NOT NULL,
    CONSTRAINT production_order_number_sequences_pkey PRIMARY KEY (company_id),
    CONSTRAINT production_order_number_sequences_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_number_sequences_last_value_check CHECK (last_value >= 0)
);

COMMENT ON TABLE indicolors.production_order_number_sequences IS
    'Último consecutivo de order_number emitido por compañía; se incrementa de forma atómica al crear una OP';
COMMENT ON COLUMN indicolors.production_order_number_sequences.company_id IS
    'Identificador de la empresa dueña del contador';
COMMENT ON COLUMN indicolors.production_order_number_sequences.last_value IS
    'Último número asignado (el order_number expuesto es OP-{last_value})';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_number_sequences TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_number_sequences TO indicolors_app;
