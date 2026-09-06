-- Agregados de progreso por proceso/ítem de una OP (inbox y listados rápidos del módulo Estación).

CREATE TABLE IF NOT EXISTS indicolors.station_process_progress (
    company_id            CHARACTER VARYING(64)       NOT NULL,
    production_order_id   CHARACTER VARYING(64)       NOT NULL,
    process_key           CHARACTER VARYING(128)      NOT NULL,
    user_id               CHARACTER VARYING(64)       NOT NULL,
    phase                 CHARACTER VARYING(32)       NOT NULL,
    catalog_item_id       CHARACTER VARYING(64),
    total_units           INTEGER                     NOT NULL DEFAULT 0,
    completed_units       INTEGER                     NOT NULL DEFAULT 0,
    delivered_units       INTEGER                     NOT NULL DEFAULT 0,
    status                CHARACTER VARYING(16)       NOT NULL DEFAULT 'pendiente',
    last_event_at         TIMESTAMP WITHOUT TIME ZONE,
    created_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT station_process_progress_pkey PRIMARY KEY (production_order_id, process_key),
    CONSTRAINT station_process_progress_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT station_process_progress_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT station_process_progress_user_fk
        FOREIGN KEY (user_id) REFERENCES indicolors.users (user_id),
    CONSTRAINT station_process_progress_status_check
        CHECK (status IN ('pendiente', 'en-proceso', 'terminado')),
    CONSTRAINT station_process_progress_units_check
        CHECK (total_units >= 0 AND completed_units >= 0 AND delivered_units >= 0)
);

CREATE INDEX IF NOT EXISTS idx_station_process_progress_user
    ON indicolors.station_process_progress (company_id, user_id, status);
CREATE INDEX IF NOT EXISTS idx_station_process_progress_company_id
    ON indicolors.station_process_progress (company_id);

COMMENT ON TABLE indicolors.station_process_progress IS 'Agregados de progreso por proceso/ítem de una OP, para inbox y listados rápidos; se recalcula desde station_operation_events y se actualiza en la misma transacción que inserta el evento. Opcional en v1: la UI puede calcular desde eventos si esta tabla no existe';

COMMENT ON COLUMN indicolors.station_process_progress.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.station_process_progress.production_order_id IS 'Identificador de la Orden de Producción a la que pertenece el proceso';
COMMENT ON COLUMN indicolors.station_process_progress.process_key IS 'Clave del proceso o ítem agregado (ver process_key en station_operation_events)';
COMMENT ON COLUMN indicolors.station_process_progress.user_id IS 'Operario asignado al proceso';
COMMENT ON COLUMN indicolors.station_process_progress.phase IS 'Etapa del wizard a la que pertenece el proceso';
COMMENT ON COLUMN indicolors.station_process_progress.catalog_item_id IS 'Referencia lógica al catálogo maestro de Terminados/Acabados (mismo valor que production_order_postpress_lines.catalog_item_id); sin FK física, validar en el Service';
COMMENT ON COLUMN indicolors.station_process_progress.total_units IS 'Unidades totales esperadas para el proceso/ítem';
COMMENT ON COLUMN indicolors.station_process_progress.completed_units IS 'Unidades procesadas acumuladas (avance_unidades)';
COMMENT ON COLUMN indicolors.station_process_progress.delivered_units IS 'Unidades entregadas acumuladas (entrega_parcial/entrega_total)';
COMMENT ON COLUMN indicolors.station_process_progress.status IS 'Estado agregado del proceso/ítem: pendiente | en-proceso | terminado';
COMMENT ON COLUMN indicolors.station_process_progress.last_event_at IS 'occurred_at del último evento que actualizó este agregado';
COMMENT ON COLUMN indicolors.station_process_progress.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.station_process_progress.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.station_process_progress TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.station_process_progress TO indicolors_app;
