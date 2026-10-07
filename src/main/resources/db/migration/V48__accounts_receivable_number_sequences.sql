-- Consecutivo atómico CXC por empresa (cxc_number = CXC-{n}).
-- Lo consumen los triggers de accounts_receivable vía fn_next_cxc_number (no el backend).

CREATE TABLE IF NOT EXISTS indicolors.accounts_receivable_number_sequences (
    company_id  CHARACTER VARYING(64) NOT NULL,
    last_value  BIGINT                NOT NULL DEFAULT 0,
    CONSTRAINT accounts_receivable_number_sequences_pkey PRIMARY KEY (company_id),
    CONSTRAINT accounts_receivable_number_sequences_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT accounts_receivable_number_sequences_last_value_check CHECK (last_value >= 0)
);

COMMENT ON TABLE indicolors.accounts_receivable_number_sequences IS
    'Último consecutivo de cxc_number emitido por compañía; se incrementa de forma atómica al crear una Cuenta por cobrar';
COMMENT ON COLUMN indicolors.accounts_receivable_number_sequences.company_id IS
    'Identificador de la empresa dueña del contador';
COMMENT ON COLUMN indicolors.accounts_receivable_number_sequences.last_value IS
    'Último número asignado (el cxc_number expuesto es CXC-{last_value})';

GRANT ALL PRIVILEGES ON TABLE indicolors.accounts_receivable_number_sequences TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.accounts_receivable_number_sequences TO indicolors_app;

CREATE OR REPLACE FUNCTION indicolors.fn_next_cxc_number(p_company_id CHARACTER VARYING)
RETURNS CHARACTER VARYING AS $$
DECLARE
    v_next BIGINT;
BEGIN
    INSERT INTO indicolors.accounts_receivable_number_sequences (company_id, last_value)
    VALUES (p_company_id, 1)
    ON CONFLICT (company_id) DO UPDATE
        SET last_value = indicolors.accounts_receivable_number_sequences.last_value + 1
    RETURNING last_value INTO v_next;

    RETURN 'CXC-' || v_next;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION indicolors.fn_next_cxc_number(CHARACTER VARYING) IS
    'Incrementa de forma atómica accounts_receivable_number_sequences para la compañía dada y devuelve el siguiente cxc_number (CXC-{n})';
