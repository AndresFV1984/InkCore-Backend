-- abonos_number (id del agregado de Abonos) = ABN-{n}.
-- Usa order_payment_number_sequences (serie propia de cuentas; distinta de LIQ).
-- Lo consumen los triggers de accounts_receivable vía fn_next_abonos_number (no el backend).

CREATE OR REPLACE FUNCTION indicolors.fn_next_abonos_number(p_company_id CHARACTER VARYING)
RETURNS CHARACTER VARYING AS $$
DECLARE
    v_next BIGINT;
BEGIN
    INSERT INTO indicolors.order_payment_number_sequences (company_id, last_value)
    VALUES (p_company_id, 1)
    ON CONFLICT (company_id) DO UPDATE
        SET last_value = indicolors.order_payment_number_sequences.last_value + 1
    RETURNING last_value INTO v_next;

    RETURN 'ABN-' || v_next;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION indicolors.fn_next_abonos_number(CHARACTER VARYING) IS
    'Reserva el siguiente ABN-{n} de order_payment_number_sequences para abonos_number (agregado). '
    'Serie propia de cuentas; distinta de LIQ-{n} (order_liq_number_sequences / payment_number).';
