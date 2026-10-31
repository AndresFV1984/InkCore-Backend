CREATE TABLE indicolors.production_order_cost_summary (
    production_order_id      CHARACTER VARYING(64)       NOT NULL,
    company_id                CHARACTER VARYING(64)       NOT NULL,
    estimated_material_cost   NUMERIC(14,2)               NOT NULL DEFAULT 0,
    estimated_machine_cost    NUMERIC(14,2)               NOT NULL DEFAULT 0,
    estimated_waste_cost      NUMERIC(14,2)               NOT NULL DEFAULT 0,
    estimated_merma_cost      NUMERIC(14,2)               NOT NULL DEFAULT 0,
    estimated_total_cost      NUMERIC(14,2)               NOT NULL DEFAULT 0,
    actual_material_cost      NUMERIC(14,2)               NOT NULL DEFAULT 0,
    actual_machine_cost       NUMERIC(14,2)               NOT NULL DEFAULT 0,
    actual_waste_cost         NUMERIC(14,2)               NOT NULL DEFAULT 0,
    actual_merma_cost         NUMERIC(14,2)               NOT NULL DEFAULT 0,
    actual_desperdicio_cost   NUMERIC(14,2)               NOT NULL DEFAULT 0,
    actual_total_cost         NUMERIC(14,2)               NOT NULL DEFAULT 0,
    quoted_price               NUMERIC(14,2),
    estimated_margin           NUMERIC(14,2) GENERATED ALWAYS AS (
        CASE WHEN quoted_price IS NULL THEN NULL ELSE quoted_price - estimated_total_cost END
    ) STORED,
    actual_margin               NUMERIC(14,2) GENERATED ALWAYS AS (
        CASE WHEN quoted_price IS NULL THEN NULL ELSE quoted_price - actual_total_cost END
    ) STORED,
    actual_margin_pct           NUMERIC(6,2) GENERATED ALWAYS AS (
        CASE WHEN quoted_price IS NULL OR quoted_price = 0 THEN NULL
             ELSE ROUND(((quoted_price - actual_total_cost) / quoted_price) * 100, 2)
        END
    ) STORED,
    updated_at                 TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT production_order_cost_summary_pkey PRIMARY KEY (production_order_id),
    CONSTRAINT production_order_cost_summary_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_cost_summary_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE
);

CREATE INDEX idx_production_order_cost_summary_company_id ON indicolors.production_order_cost_summary (company_id);
CREATE INDEX idx_production_order_cost_summary_actual_margin_pct ON indicolors.production_order_cost_summary (company_id, actual_margin_pct);

COMMENT ON TABLE indicolors.production_order_cost_summary IS 'Resumen derivado de costos por Orden de Producción (1:1 con production_orders), recalculado automáticamente por triggers al escribir en paper_rows, prepress_details, postpress_lines, machine_usage o waste_records. Es la fuente para el reporte de Rentabilidad por Orden (costo cotizado vs. costo real, margen real)';
COMMENT ON COLUMN indicolors.production_order_cost_summary.production_order_id IS 'Identificador de la Orden de Producción (mismo id que production_orders, relación 1:1)';
COMMENT ON COLUMN indicolors.production_order_cost_summary.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_cost_summary.estimated_material_cost IS 'Papel + corte + planchas + diseño + terminados/acabados, tomado de las tablas fuente (es el mismo valor que actual_material_cost hasta que el sistema capture consumo real de material distinto al cotizado)';
COMMENT ON COLUMN indicolors.production_order_cost_summary.estimated_machine_cost IS 'Suma de production_order_machine_usage.estimated_machine_cost de todas las fases de la OP';
COMMENT ON COLUMN indicolors.production_order_cost_summary.estimated_waste_cost IS 'Suma de planned_cost de todos los registros. El desperdicio aporta 0 porque su planned_quantity es 0';
COMMENT ON COLUMN indicolors.production_order_cost_summary.estimated_merma_cost IS 'Suma de planned_cost donde waste_category es distinto de desperdicio';
COMMENT ON COLUMN indicolors.production_order_cost_summary.estimated_total_cost IS 'estimated_material_cost + estimated_machine_cost + estimated_waste_cost';
COMMENT ON COLUMN indicolors.production_order_cost_summary.actual_material_cost IS 'Igual a estimated_material_cost (ver comentario de esa columna); se deja separado para permitir a futuro capturar consumo real de material distinto al cotizado sin cambiar el esquema';
COMMENT ON COLUMN indicolors.production_order_cost_summary.actual_machine_cost IS 'Suma de production_order_machine_usage.actual_machine_cost cuando existe; si una fase aún no tiene dato real, se usa su estimated_machine_cost como mejor aproximación disponible';
COMMENT ON COLUMN indicolors.production_order_cost_summary.actual_waste_cost IS 'Suma de COALESCE(actual_cost, planned_cost) de todos los registros. Incluye merma real y desperdicio';
COMMENT ON COLUMN indicolors.production_order_cost_summary.actual_merma_cost IS 'Suma de COALESCE(actual_cost, planned_cost) donde waste_category es distinto de desperdicio';
COMMENT ON COLUMN indicolors.production_order_cost_summary.actual_desperdicio_cost IS 'Suma de COALESCE(actual_cost, 0) donde waste_category = desperdicio. No entra en el precio cobrado; sí entra en actual_waste_cost y por tanto en actual_total_cost';
COMMENT ON COLUMN indicolors.production_order_cost_summary.actual_total_cost IS 'actual_material_cost + actual_machine_cost + actual_waste_cost';
COMMENT ON COLUMN indicolors.production_order_cost_summary.quoted_price IS
    'Precio final cotizado al cliente (= totalToCharge del panel Cobro). Lo escribe el backend al cotizar '
    '(no este trigger) y no cambia al registrar desperdicio o retrabajo. '
    'Fuente de totalToCharge en GET /production-orders/list (sin recalcular el agregado).';
COMMENT ON COLUMN indicolors.production_order_cost_summary.estimated_margin IS 'quoted_price - estimated_total_cost; NULL mientras no se conozca quoted_price';
COMMENT ON COLUMN indicolors.production_order_cost_summary.actual_margin IS 'quoted_price - actual_total_cost; NULL mientras no se conozca quoted_price. Esta es la ganancia neta real de la OP, no el margen que se puso en la cotización';
COMMENT ON COLUMN indicolors.production_order_cost_summary.actual_margin_pct IS 'Margen real como % del precio cotizado; usado para resaltar en el reporte las órdenes que están dejando pérdida';
COMMENT ON COLUMN indicolors.production_order_cost_summary.updated_at IS 'Fecha y hora del último recálculo automático';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_cost_summary TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_cost_summary TO indicolors_app;

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

