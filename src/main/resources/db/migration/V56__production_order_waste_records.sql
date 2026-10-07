CREATE TABLE indicolors.production_order_waste_records (
    production_order_waste_record_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                       CHARACTER VARYING(64)       NOT NULL,
    production_order_id              CHARACTER VARYING(64)       NOT NULL,
    phase                            CHARACTER VARYING(20)       NOT NULL,
    waste_category                   CHARACTER VARYING(20)       NOT NULL,
    waste_origin                     CHARACTER VARYING(20)       NOT NULL DEFAULT 'exceso',
    material_type                    CHARACTER VARYING(20)       NOT NULL,
    paper_row_id                     CHARACTER VARYING(64),
    postpress_line_id                CHARACTER VARYING(64),
    planned_quantity                 NUMERIC(12,2)                NOT NULL DEFAULT 0,
    actual_quantity                  NUMERIC(12,2),
    unit_cost_snapshot               NUMERIC(12,2)                NOT NULL,
    planned_cost                     NUMERIC(12,2) GENERATED ALWAYS AS (planned_quantity * unit_cost_snapshot) STORED,
    actual_cost                      NUMERIC(12,2) GENERATED ALWAYS AS (
        CASE WHEN actual_quantity IS NULL THEN NULL ELSE actual_quantity * unit_cost_snapshot END
    ) STORED,
    note                             TEXT,
    created_at                       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT production_order_waste_records_pkey PRIMARY KEY (production_order_waste_record_id),
    CONSTRAINT production_order_waste_records_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_waste_records_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_waste_records_paper_row_fk
        FOREIGN KEY (paper_row_id) REFERENCES indicolors.production_order_paper_rows (production_order_paper_row_id) ON DELETE SET NULL,
    CONSTRAINT production_order_waste_records_postpress_line_fk
        FOREIGN KEY (postpress_line_id) REFERENCES indicolors.production_order_postpress_lines (production_order_postpress_line_id) ON DELETE SET NULL,
    CONSTRAINT production_order_waste_records_phase_check
        CHECK (phase IN ('preprensa','corte-papel','impresion','terminados','acabados')),
    CONSTRAINT production_order_waste_records_category_check
        CHECK (waste_category IN ('merma_corte','merma_operativa','merma_administrativa','desperdicio')),
    CONSTRAINT production_order_waste_records_origin_check
        CHECK (waste_origin IN ('exceso','retrabajo')),
    CONSTRAINT production_order_waste_records_material_type_check
        CHECK (material_type IN ('papel','tinta','plancha','acabado','otro')),
    CONSTRAINT production_order_waste_records_planned_quantity_check CHECK (planned_quantity >= 0),
    CONSTRAINT production_order_waste_records_actual_quantity_check CHECK (actual_quantity IS NULL OR actual_quantity >= 0),
    CONSTRAINT production_order_waste_records_unit_cost_check CHECK (unit_cost_snapshot >= 0)
);

CREATE INDEX idx_production_order_waste_records_company_id ON indicolors.production_order_waste_records (company_id);
CREATE INDEX idx_production_order_waste_records_order_id ON indicolors.production_order_waste_records (production_order_id);
CREATE INDEX idx_production_order_waste_records_phase ON indicolors.production_order_waste_records (production_order_id, phase);
CREATE INDEX idx_production_order_waste_records_category ON indicolors.production_order_waste_records (waste_category);
CREATE INDEX idx_production_order_waste_records_paper_row_id ON indicolors.production_order_waste_records (paper_row_id);
CREATE INDEX idx_production_order_waste_records_postpress_line_id ON indicolors.production_order_waste_records (postpress_line_id);

COMMENT ON TABLE indicolors.production_order_waste_records IS 'Mermas (planificables: corte/operativa/administrativa) y desperdicios (no planificados) valorizados por fase de la Orden de Producción; base para comparar costo cotizado vs. costo real';
COMMENT ON COLUMN indicolors.production_order_waste_records.production_order_waste_record_id IS 'Identificador único del registro de merma/desperdicio';
COMMENT ON COLUMN indicolors.production_order_waste_records.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_waste_records.production_order_id IS 'Identificador de la Orden de Producción a la que pertenece';
COMMENT ON COLUMN indicolors.production_order_waste_records.phase IS 'Fase del wizard en la que ocurre la merma/desperdicio';
COMMENT ON COLUMN indicolors.production_order_waste_records.waste_category IS 'merma_corte y merma_operativa son planificables/predecibles (se cotizan de antemano); merma_administrativa es diferencia de inventario/almacenaje; desperdicio es lo NO planificado, la fuga real de rentabilidad';
COMMENT ON COLUMN indicolors.production_order_waste_records.waste_origin IS 'exceso = lo que supera la merma al cerrar la fase; retrabajo = reimpresión o reproceso pagado por el taller. La aplicación solo distingue el origen cuando waste_category = desperdicio; el resto queda en exceso';
COMMENT ON COLUMN indicolors.production_order_waste_records.material_type IS 'Tipo de insumo afectado por la merma/desperdicio';
COMMENT ON COLUMN indicolors.production_order_waste_records.paper_row_id IS 'Fila de corte de papel asociada, cuando material_type=papel (FK opcional a production_order_paper_rows)';
COMMENT ON COLUMN indicolors.production_order_waste_records.postpress_line_id IS 'Línea de Terminados/Acabados asociada, cuando material_type=acabado (FK opcional a production_order_postpress_lines)';
COMMENT ON COLUMN indicolors.production_order_waste_records.planned_quantity IS 'Cantidad planificada como merma al cotizar (pliegos, unidades o piezas según material_type)';
COMMENT ON COLUMN indicolors.production_order_waste_records.actual_quantity IS 'Cantidad real observada al cerrar la fase; NULL hasta que se registre en el módulo Estación';
COMMENT ON COLUMN indicolors.production_order_waste_records.unit_cost_snapshot IS 'Costo unitario del material en el momento del registro (snapshot)';
COMMENT ON COLUMN indicolors.production_order_waste_records.planned_cost IS 'Costo de la merma planificada, calculado automáticamente: planned_quantity * unit_cost_snapshot';
COMMENT ON COLUMN indicolors.production_order_waste_records.actual_cost IS 'Costo de la merma/desperdicio real, calculado automáticamente; NULL mientras no se capture actual_quantity';
COMMENT ON COLUMN indicolors.production_order_waste_records.note IS 'Motivo (ej. papel de mala calidad, mal cortado, ajuste de registro/color, cambio a medio tiro, defecto de impresión, cambio de especificación del cliente)';
COMMENT ON COLUMN indicolors.production_order_waste_records.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_waste_records.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_waste_records TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_waste_records TO indicolors_app;
