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
COMMENT ON COLUMN indicolors.production_order_cost_summary.quoted_price IS 'Precio final cotizado/facturado al cliente; lo actualiza el backend (no un trigger) una vez resuelta la lógica de descuentos de production_order_billing_details';
COMMENT ON COLUMN indicolors.production_order_cost_summary.estimated_margin IS 'quoted_price - estimated_total_cost; NULL mientras no se conozca quoted_price';
COMMENT ON COLUMN indicolors.production_order_cost_summary.actual_margin IS 'quoted_price - actual_total_cost; NULL mientras no se conozca quoted_price. Esta es la ganancia neta real de la OP, no el margen que se puso en la cotización';
COMMENT ON COLUMN indicolors.production_order_cost_summary.actual_margin_pct IS 'Margen real como % del precio cotizado; usado para resaltar en el reporte las órdenes que están dejando pérdida';
COMMENT ON COLUMN indicolors.production_order_cost_summary.updated_at IS 'Fecha y hora del último recálculo automático';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_cost_summary TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_cost_summary TO indicolors_app;
