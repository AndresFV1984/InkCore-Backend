CREATE TABLE indicolors.production_order_machine_usage (
    production_order_machine_usage_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                        CHARACTER VARYING(64)       NOT NULL,
    production_order_id               CHARACTER VARYING(64)       NOT NULL,
    phase                              CHARACTER VARYING(20)       NOT NULL,
    machine_id                        CHARACTER VARYING(64)       NOT NULL,
    machine_name_snapshot             CHARACTER VARYING(150)      NOT NULL,
    cost_per_hour_snapshot            NUMERIC(12,2)               NOT NULL,
    estimated_setup_minutes           INTEGER                     NOT NULL DEFAULT 0,
    estimated_run_minutes             INTEGER                     NOT NULL DEFAULT 0,
    estimated_machine_cost            NUMERIC(12,2) GENERATED ALWAYS AS (
        ((estimated_setup_minutes + estimated_run_minutes) / 60.0) * cost_per_hour_snapshot
    ) STORED,
    actual_setup_minutes              INTEGER,
    actual_run_minutes                INTEGER,
    actual_machine_cost               NUMERIC(12,2) GENERATED ALWAYS AS (
        CASE
            WHEN actual_setup_minutes IS NULL AND actual_run_minutes IS NULL THEN NULL
            ELSE ((COALESCE(actual_setup_minutes, 0) + COALESCE(actual_run_minutes, 0)) / 60.0) * cost_per_hour_snapshot
        END
    ) STORED,
    created_at                        TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                        TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT production_order_machine_usage_pkey PRIMARY KEY (production_order_machine_usage_id),
    CONSTRAINT production_order_machine_usage_order_phase_machine_unique
        UNIQUE (production_order_id, phase, machine_id),
    CONSTRAINT production_order_machine_usage_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_machine_usage_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_machine_usage_machine_fk
        FOREIGN KEY (machine_id) REFERENCES indicolors.machines (machine_id),
    CONSTRAINT production_order_machine_usage_phase_check
        CHECK (phase IN ('preprensa','corte-papel','impresion','terminados','acabados')),
    CONSTRAINT production_order_machine_usage_estimated_setup_check CHECK (estimated_setup_minutes >= 0),
    CONSTRAINT production_order_machine_usage_estimated_run_check CHECK (estimated_run_minutes >= 0),
    CONSTRAINT production_order_machine_usage_actual_setup_check CHECK (actual_setup_minutes IS NULL OR actual_setup_minutes >= 0),
    CONSTRAINT production_order_machine_usage_actual_run_check CHECK (actual_run_minutes IS NULL OR actual_run_minutes >= 0)
);

CREATE INDEX idx_production_order_machine_usage_company_id ON indicolors.production_order_machine_usage (company_id);
CREATE INDEX idx_production_order_machine_usage_order_id ON indicolors.production_order_machine_usage (production_order_id);
CREATE INDEX idx_production_order_machine_usage_machine_id ON indicolors.production_order_machine_usage (machine_id);
CREATE INDEX idx_production_order_machine_usage_phase ON indicolors.production_order_machine_usage (production_order_id, phase);

COMMENT ON TABLE indicolors.production_order_machine_usage IS 'Uso de máquina por fase de una Orden de Producción (estimado al cotizar, real al cerrar la fase). Esta es la tabla que faltaba para dejar de costear solo por cantidad solicitada + material, y sí cargar el costo fijo real de la máquina en cada operación';
COMMENT ON COLUMN indicolors.production_order_machine_usage.production_order_machine_usage_id IS 'Identificador único del registro de uso de máquina';
COMMENT ON COLUMN indicolors.production_order_machine_usage.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_machine_usage.production_order_id IS 'Identificador de la Orden de Producción a la que pertenece';
COMMENT ON COLUMN indicolors.production_order_machine_usage.phase IS 'Fase del wizard en la que se usó la máquina (mismo dominio que station_operation_events.phase)';
COMMENT ON COLUMN indicolors.production_order_machine_usage.machine_id IS 'Identificador de la máquina usada (FK a machines)';
COMMENT ON COLUMN indicolors.production_order_machine_usage.machine_name_snapshot IS 'Snapshot del nombre de la máquina al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_machine_usage.cost_per_hour_snapshot IS 'Snapshot del costo/hora de la máquina al momento de guardar; no debe recalcularse si luego cambia la tarifa de la máquina, para no alterar el costo histórico de la OP';
COMMENT ON COLUMN indicolors.production_order_machine_usage.estimated_setup_minutes IS 'Minutos estimados de arranque/calibración (montaje, registro de color) al cotizar';
COMMENT ON COLUMN indicolors.production_order_machine_usage.estimated_run_minutes IS 'Minutos estimados de producción efectiva al cotizar';
COMMENT ON COLUMN indicolors.production_order_machine_usage.estimated_machine_cost IS 'Costo de máquina estimado, calculado automáticamente: (minutos de arranque + minutos de producción) / 60 * costo/hora snapshot';
COMMENT ON COLUMN indicolors.production_order_machine_usage.actual_setup_minutes IS 'Minutos reales de arranque, capturados al cerrar la fase en el módulo Estación; NULL hasta que se registre';
COMMENT ON COLUMN indicolors.production_order_machine_usage.actual_run_minutes IS 'Minutos reales de producción, capturados al cerrar la fase; NULL hasta que se registre';
COMMENT ON COLUMN indicolors.production_order_machine_usage.actual_machine_cost IS 'Costo de máquina real, calculado automáticamente igual que el estimado pero con minutos reales; NULL mientras no se haya capturado ningún dato real';
COMMENT ON COLUMN indicolors.production_order_machine_usage.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_machine_usage.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_machine_usage TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_machine_usage TO indicolors_app;
