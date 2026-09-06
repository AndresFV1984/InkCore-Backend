-- Bitácora operativa append-only del módulo Estación (fuente de verdad de eventos en planta).

CREATE TABLE IF NOT EXISTS indicolors.station_operation_events (
    station_operation_event_id   CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                   CHARACTER VARYING(64)       NOT NULL,
    production_order_id          CHARACTER VARYING(64),
    client_id                    CHARACTER VARYING(64),
    user_id                      CHARACTER VARYING(64)       NOT NULL,
    actor_user_id                CHARACTER VARYING(64)       NOT NULL,
    actor_name                   CHARACTER VARYING(255),
    work_name                    CHARACTER VARYING(150),
    phase                        CHARACTER VARYING(32)       NOT NULL,
    process_key                  CHARACTER VARYING(128)      NOT NULL,
    catalog_item_kind            CHARACTER VARYING(16),
    catalog_item_id              CHARACTER VARYING(64),
    catalog_item_label           CHARACTER VARYING(255),
    event_type                   CHARACTER VARYING(32)       NOT NULL,
    occurred_at                  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    units                        INTEGER,
    pause_reason                 CHARACTER VARYING(64),
    note                         TEXT,
    production_status_snapshot   CHARACTER VARYING(64),
    order_status_snapshot        CHARACTER VARYING(64),
    is_shift_event               BOOLEAN                     NOT NULL DEFAULT FALSE,
    created_at                   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT station_operation_events_pkey PRIMARY KEY (station_operation_event_id),
    CONSTRAINT station_operation_events_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT station_operation_events_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id),
    CONSTRAINT station_operation_events_client_fk
        FOREIGN KEY (client_id) REFERENCES indicolors.clients (client_id),
    CONSTRAINT station_operation_events_user_fk
        FOREIGN KEY (user_id) REFERENCES indicolors.users (user_id),
    CONSTRAINT station_operation_events_actor_user_fk
        FOREIGN KEY (actor_user_id) REFERENCES indicolors.users (user_id),
    CONSTRAINT station_operation_events_order_or_shift_check
        CHECK (production_order_id IS NOT NULL OR is_shift_event = TRUE),
    CONSTRAINT station_operation_events_event_type_check CHECK (
        event_type IN (
            'asignacion', 'cambio_estado_orden', 'entrega_parcial', 'entrega_total',
            'avance_unidades', 'marca_horario', 'inicio_fase', 'fin_fase',
            'paro', 'reanudacion'
        )
    ),
    CONSTRAINT station_operation_events_catalog_item_kind_check
        CHECK (catalog_item_kind IS NULL OR catalog_item_kind IN ('terminado', 'acabado'))
);

CREATE INDEX IF NOT EXISTS idx_station_operation_events_company_id
    ON indicolors.station_operation_events (company_id);
CREATE INDEX IF NOT EXISTS idx_station_operation_events_order_time
    ON indicolors.station_operation_events (company_id, production_order_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_station_operation_events_user_time
    ON indicolors.station_operation_events (company_id, user_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_station_operation_events_client_time
    ON indicolors.station_operation_events (company_id, client_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_station_operation_events_process
    ON indicolors.station_operation_events (production_order_id, process_key, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_station_operation_events_catalog_item
    ON indicolors.station_operation_events (production_order_id, catalog_item_kind, catalog_item_id, occurred_at DESC)
    WHERE catalog_item_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_station_operation_events_event_type
    ON indicolors.station_operation_events (company_id, event_type, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_station_operation_events_shift
    ON indicolors.station_operation_events (company_id, user_id, occurred_at DESC)
    WHERE is_shift_event = TRUE;

COMMENT ON TABLE indicolors.station_operation_events IS 'Bitácora operativa append-only del módulo Estación: cada fila es un hecho ocurrido en planta (avance, pausa, entrega, jornada). No admite UPDATE ni DELETE en producción; correcciones = nuevo evento con note explicativa';

COMMENT ON COLUMN indicolors.station_operation_events.station_operation_event_id IS 'Identificador único del evento';
COMMENT ON COLUMN indicolors.station_operation_events.company_id IS 'Identificador de la empresa dueña del evento';
COMMENT ON COLUMN indicolors.station_operation_events.production_order_id IS 'Identificador de la Orden de Producción asociada; NULL solo si is_shift_event = TRUE (jornada sin OP)';
COMMENT ON COLUMN indicolors.station_operation_events.client_id IS 'Snapshot del cliente de la OP al momento de insertar; obligatorio si hay OP';
COMMENT ON COLUMN indicolors.station_operation_events.user_id IS 'Operario asignado al proceso sobre el que ocurre el evento';
COMMENT ON COLUMN indicolors.station_operation_events.actor_user_id IS 'Usuario que ejecutó la acción (tomado del JWT), puede diferir del operario asignado';
COMMENT ON COLUMN indicolors.station_operation_events.actor_name IS 'Snapshot del nombre del actor al momento de insertar (reportes históricos)';
COMMENT ON COLUMN indicolors.station_operation_events.work_name IS 'Snapshot del nombre del trabajo/pieza de la OP al momento de insertar';
COMMENT ON COLUMN indicolors.station_operation_events.phase IS 'Etapa del wizard sobre la que ocurre el evento (preprensa, corte-papel, impresion, terminados, acabados, jornada)';
COMMENT ON COLUMN indicolors.station_operation_events.process_key IS 'Clave exacta del proceso o ítem: nombre de fase, fase plural de catálogo, terminado:{catalogItemId}, acabado:{catalogItemId} o jornada. El catalogItemId es el id del catálogo maestro (mismo valor que production_order_postpress_lines.catalog_item_id), no el id de production_order_postpress_records';
COMMENT ON COLUMN indicolors.station_operation_events.catalog_item_kind IS 'Tipo de ítem de catálogo cuando process_key referencia uno: terminado | acabado';
COMMENT ON COLUMN indicolors.station_operation_events.catalog_item_id IS 'Referencia lógica al catálogo maestro de Terminados/Acabados (mismo valor que production_order_postpress_lines.catalog_item_id); sin FK física por no ser una tabla única de origen, validar en el Service que exista al menos una línea de la OP con este catalog_item_id';
COMMENT ON COLUMN indicolors.station_operation_events.catalog_item_label IS 'Snapshot de la etiqueta del ítem de catálogo al momento de insertar';
COMMENT ON COLUMN indicolors.station_operation_events.event_type IS 'Tipo de hecho operativo registrado';
COMMENT ON COLUMN indicolors.station_operation_events.occurred_at IS 'Fecha/hora del hecho operativo (puede venir del cliente; el backend valida que no sea futuro lejano)';
COMMENT ON COLUMN indicolors.station_operation_events.units IS 'Unidades involucradas; solo aplica en avance_unidades, entrega_parcial y entrega_total';
COMMENT ON COLUMN indicolors.station_operation_events.pause_reason IS 'Motivo de la pausa; solo aplica en paro y marca_horario';
COMMENT ON COLUMN indicolors.station_operation_events.note IS 'Nota libre; usada también para explicar correcciones sobre eventos previos';
COMMENT ON COLUMN indicolors.station_operation_events.production_status_snapshot IS 'Snapshot del estado de producción en planta al momento del evento';
COMMENT ON COLUMN indicolors.station_operation_events.order_status_snapshot IS 'Snapshot de production_orders.status al momento del evento';
COMMENT ON COLUMN indicolors.station_operation_events.is_shift_event IS 'True=evento de jornada laboral sin OP asociada (orderId = __jornada__ en el SPA)';
COMMENT ON COLUMN indicolors.station_operation_events.created_at IS 'Fecha y hora de persistencia del registro (distinta de occurred_at)';

GRANT ALL PRIVILEGES ON TABLE indicolors.station_operation_events TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.station_operation_events TO indicolors_app;
