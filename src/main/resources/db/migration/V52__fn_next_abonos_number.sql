-- abonos_number (id del agregado de Abonos) = ABN-{n}, mismo prefijo que payment_number.
-- Comparte order_payment_number_sequences (V48) para garantizar que, por compañía,
-- abonos_number nunca coincida con un payment_number (movimientos). Sin tabla aparte.
-- Lo consumen los triggers de accounts_receivable (V53) vía fn_next_abonos_number (no el backend).

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
    'Misma secuencia que payment_number: el id del agregado nunca choca con un movimiento.';
