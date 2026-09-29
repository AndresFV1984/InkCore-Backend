CREATE OR REPLACE FUNCTION indicolors.fn_recalc_production_order_cost_summary(
    p_production_order_id CHARACTER VARYING(64),
    p_company_id           CHARACTER VARYING(64)
)
RETURNS VOID AS $$
DECLARE
    v_material_cost        NUMERIC(14,2);
    v_machine_est          NUMERIC(14,2);
    v_machine_act          NUMERIC(14,2);
    v_waste_est            NUMERIC(14,2);
    v_waste_act            NUMERIC(14,2);
    v_merma_est            NUMERIC(14,2);
    v_merma_act            NUMERIC(14,2);
    v_desperdicio_act      NUMERIC(14,2);
    v_quoted_price         NUMERIC(14,2);
BEGIN
    -- Material: papel + corte (paper_rows) + planchas/diseño (prepress_details)
    -- + terminados/acabados (postpress_lines vía postpress_records)
    SELECT COALESCE(SUM(pr.total_paper_value), 0) + COALESCE(SUM(pr.total_cut_value), 0)
    INTO v_material_cost
    FROM indicolors.production_order_paper_rows pr
    WHERE pr.production_order_id = p_production_order_id;

    v_material_cost := v_material_cost
        + COALESCE((
            SELECT COALESCE(pd.total_plates_value, 0)
                 + COALESCE(pd.design_cost, 0)
                 + COALESCE(pd.new_plate_cost, 0)
            FROM indicolors.production_order_prepress_details pd
            WHERE pd.production_order_id = p_production_order_id
          ), 0)
        + COALESCE((
            SELECT SUM(pl.charged_price)
            FROM indicolors.production_order_postpress_lines pl
            JOIN indicolors.production_order_postpress_records rec
                ON rec.production_order_postpress_record_id = pl.record_id
            WHERE rec.production_order_id = p_production_order_id
          ), 0);

    -- Máquina: estimado vs real (real cae al estimado mientras no se capture)
    SELECT COALESCE(SUM(mu.estimated_machine_cost), 0),
           COALESCE(SUM(COALESCE(mu.actual_machine_cost, mu.estimated_machine_cost)), 0)
    INTO v_machine_est, v_machine_act
    FROM indicolors.production_order_machine_usage mu
    WHERE mu.production_order_id = p_production_order_id;

    -- Merma + desperdicio juntos. El desperdicio aporta 0 al estimado
    -- porque su planned_quantity es 0. El real incluye merma y desperdicio.
    SELECT COALESCE(SUM(wr.planned_cost), 0),
           COALESCE(SUM(COALESCE(wr.actual_cost, wr.planned_cost)), 0)
    INTO v_waste_est, v_waste_act
    FROM indicolors.production_order_waste_records wr
    WHERE wr.production_order_id = p_production_order_id;

    SELECT COALESCE(SUM(wr.planned_cost), 0),
           COALESCE(SUM(COALESCE(wr.actual_cost, wr.planned_cost)), 0)
    INTO v_merma_est, v_merma_act
    FROM indicolors.production_order_waste_records wr
    WHERE wr.production_order_id = p_production_order_id
      AND wr.waste_category <> 'desperdicio';

    SELECT COALESCE(SUM(COALESCE(wr.actual_cost, 0)), 0)
    INTO v_desperdicio_act
    FROM indicolors.production_order_waste_records wr
    WHERE wr.production_order_id = p_production_order_id
      AND wr.waste_category = 'desperdicio';

    -- Conserva el quoted_price que ya estuviera guardado (lo fija el backend,
    -- no este trigger)
    SELECT cs.quoted_price INTO v_quoted_price
    FROM indicolors.production_order_cost_summary cs
    WHERE cs.production_order_id = p_production_order_id;

    INSERT INTO indicolors.production_order_cost_summary (
        production_order_id, company_id,
        estimated_material_cost, estimated_machine_cost, estimated_waste_cost, estimated_merma_cost, estimated_total_cost,
        actual_material_cost, actual_machine_cost, actual_waste_cost, actual_merma_cost, actual_desperdicio_cost, actual_total_cost,
        quoted_price, updated_at
    )
    VALUES (
        p_production_order_id, p_company_id,
        v_material_cost, v_machine_est, v_waste_est, v_merma_est, v_material_cost + v_machine_est + v_waste_est,
        v_material_cost, v_machine_act, v_waste_act, v_merma_act, v_desperdicio_act, v_material_cost + v_machine_act + v_waste_act,
        v_quoted_price, now()
    )
    ON CONFLICT (production_order_id) DO UPDATE SET
        estimated_material_cost = EXCLUDED.estimated_material_cost,
        estimated_machine_cost  = EXCLUDED.estimated_machine_cost,
        estimated_waste_cost    = EXCLUDED.estimated_waste_cost,
        estimated_merma_cost    = EXCLUDED.estimated_merma_cost,
        estimated_total_cost    = EXCLUDED.estimated_total_cost,
        actual_material_cost    = EXCLUDED.actual_material_cost,
        actual_machine_cost     = EXCLUDED.actual_machine_cost,
        actual_waste_cost       = EXCLUDED.actual_waste_cost,
        actual_merma_cost       = EXCLUDED.actual_merma_cost,
        actual_desperdicio_cost = EXCLUDED.actual_desperdicio_cost,
        actual_total_cost       = EXCLUDED.actual_total_cost,
        updated_at              = now();
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION indicolors.fn_recalc_production_order_cost_summary(CHARACTER VARYING, CHARACTER VARYING) IS
    'Recalcula y hace upsert de production_order_cost_summary para una OP a partir de paper_rows, prepress_details, postpress_lines, machine_usage y waste_records. Separa merma y desperdicio sin cambiar los totales que alimentan el margen. Se invoca desde triggers en cada tabla fuente, siempre dentro de la misma transacción de la escritura original';

-- --- Trigger wrappers por tabla fuente (cada uno resuelve production_order_id
-- --- y company_id según su propia estructura, y llama a la función compartida) ---

CREATE OR REPLACE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_paper_rows()
RETURNS TRIGGER AS $$
DECLARE
    v_order_id CHARACTER VARYING(64);
    v_company_id CHARACTER VARYING(64);
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_order_id := OLD.production_order_id;
        v_company_id := OLD.company_id;
    ELSE
        v_order_id := NEW.production_order_id;
        v_company_id := NEW.company_id;
    END IF;
    PERFORM indicolors.fn_recalc_production_order_cost_summary(v_order_id, v_company_id);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_paper_rows_recalc_cost_summary ON indicolors.production_order_paper_rows;
CREATE TRIGGER trg_paper_rows_recalc_cost_summary
    AFTER INSERT OR UPDATE OR DELETE ON indicolors.production_order_paper_rows
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_paper_rows();

CREATE OR REPLACE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_prepress_details()
RETURNS TRIGGER AS $$
DECLARE
    v_order_id CHARACTER VARYING(64);
    v_company_id CHARACTER VARYING(64);
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_order_id := OLD.production_order_id;
        v_company_id := OLD.company_id;
    ELSE
        v_order_id := NEW.production_order_id;
        v_company_id := NEW.company_id;
    END IF;
    PERFORM indicolors.fn_recalc_production_order_cost_summary(v_order_id, v_company_id);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prepress_details_recalc_cost_summary ON indicolors.production_order_prepress_details;
CREATE TRIGGER trg_prepress_details_recalc_cost_summary
    AFTER INSERT OR UPDATE OR DELETE ON indicolors.production_order_prepress_details
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_prepress_details();

CREATE OR REPLACE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_postpress_lines()
RETURNS TRIGGER AS $$
DECLARE
    v_order_id CHARACTER VARYING(64);
    v_company_id CHARACTER VARYING(64);
    v_record_id CHARACTER VARYING(64);
BEGIN
    v_record_id := CASE WHEN TG_OP = 'DELETE' THEN OLD.record_id ELSE NEW.record_id END;
    v_company_id := CASE WHEN TG_OP = 'DELETE' THEN OLD.company_id ELSE NEW.company_id END;

    SELECT rec.production_order_id INTO v_order_id
    FROM indicolors.production_order_postpress_records rec
    WHERE rec.production_order_postpress_record_id = v_record_id;

    IF v_order_id IS NOT NULL THEN
        PERFORM indicolors.fn_recalc_production_order_cost_summary(v_order_id, v_company_id);
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_postpress_lines_recalc_cost_summary ON indicolors.production_order_postpress_lines;
CREATE TRIGGER trg_postpress_lines_recalc_cost_summary
    AFTER INSERT OR UPDATE OR DELETE ON indicolors.production_order_postpress_lines
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_postpress_lines();

CREATE OR REPLACE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_machine_usage()
RETURNS TRIGGER AS $$
DECLARE
    v_order_id CHARACTER VARYING(64);
    v_company_id CHARACTER VARYING(64);
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_order_id := OLD.production_order_id;
        v_company_id := OLD.company_id;
    ELSE
        v_order_id := NEW.production_order_id;
        v_company_id := NEW.company_id;
    END IF;
    PERFORM indicolors.fn_recalc_production_order_cost_summary(v_order_id, v_company_id);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_machine_usage_recalc_cost_summary ON indicolors.production_order_machine_usage;
CREATE TRIGGER trg_machine_usage_recalc_cost_summary
    AFTER INSERT OR UPDATE OR DELETE ON indicolors.production_order_machine_usage
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_machine_usage();

CREATE OR REPLACE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_waste_records()
RETURNS TRIGGER AS $$
DECLARE
    v_order_id CHARACTER VARYING(64);
    v_company_id CHARACTER VARYING(64);
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_order_id := OLD.production_order_id;
        v_company_id := OLD.company_id;
    ELSE
        v_order_id := NEW.production_order_id;
        v_company_id := NEW.company_id;
    END IF;
    PERFORM indicolors.fn_recalc_production_order_cost_summary(v_order_id, v_company_id);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_waste_records_recalc_cost_summary ON indicolors.production_order_waste_records;
CREATE TRIGGER trg_waste_records_recalc_cost_summary
    AFTER INSERT OR UPDATE OR DELETE ON indicolors.production_order_waste_records
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_waste_records();

COMMENT ON FUNCTION indicolors.fn_trg_recalc_cost_summary_from_paper_rows() IS 'Dispara el recálculo de production_order_cost_summary tras cualquier cambio en production_order_paper_rows';
COMMENT ON FUNCTION indicolors.fn_trg_recalc_cost_summary_from_prepress_details() IS 'Dispara el recálculo de production_order_cost_summary tras cualquier cambio en production_order_prepress_details';
COMMENT ON FUNCTION indicolors.fn_trg_recalc_cost_summary_from_postpress_lines() IS 'Dispara el recálculo de production_order_cost_summary tras cualquier cambio en production_order_postpress_lines, resolviendo production_order_id vía production_order_postpress_records';
COMMENT ON FUNCTION indicolors.fn_trg_recalc_cost_summary_from_machine_usage() IS 'Dispara el recálculo de production_order_cost_summary tras cualquier cambio en production_order_machine_usage';
COMMENT ON FUNCTION indicolors.fn_trg_recalc_cost_summary_from_waste_records() IS 'Dispara el recálculo de production_order_cost_summary tras cualquier cambio en production_order_waste_records';

-- Backfill para OP ya existentes: el trigger solo corre en escrituras futuras.
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN SELECT production_order_id, company_id FROM indicolors.production_orders LOOP
        PERFORM indicolors.fn_recalc_production_order_cost_summary(r.production_order_id, r.company_id);
    END LOOP;
END $$;
