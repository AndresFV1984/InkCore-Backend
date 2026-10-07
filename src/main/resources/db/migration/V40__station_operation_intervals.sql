-- Intervalos de labor/pausa/jornada materializados a partir de station_operation_events.

CREATE TABLE IF NOT EXISTS indicolors.station_operation_intervals (
    station_operation_interval_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                    CHARACTER VARYING(64)       NOT NULL,
    production_order_id           CHARACTER VARYING(64),
    client_id                     CHARACTER VARYING(64),
    user_id                       CHARACTER VARYING(64)       NOT NULL,
    process_key                   CHARACTER VARYING(128),
    phase                         CHARACTER VARYING(32),
    catalog_item_id               CHARACTER VARYING(64),
    interval_kind                 CHARACTER VARYING(16)       NOT NULL,
    started_at                    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    ended_at                      TIMESTAMP WITHOUT TIME ZONE,
    duration_ms                   BIGINT,
    pause_reason                  CHARACTER VARYING(64),
    note                          TEXT,
    opened_by_event_id            CHARACTER VARYING(64)       NOT NULL,
    closed_by_event_id            CHARACTER VARYING(64),
    is_open                       BOOLEAN                     NOT NULL DEFAULT TRUE,
    created_at                    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT station_operation_intervals_pkey PRIMARY KEY (station_operation_interval_id),
    CONSTRAINT station_operation_intervals_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT station_operation_intervals_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id),
    CONSTRAINT station_operation_intervals_client_fk
        FOREIGN KEY (client_id) REFERENCES indicolors.clients (client_id),
    CONSTRAINT station_operation_intervals_user_fk
        FOREIGN KEY (user_id) REFERENCES indicolors.users (user_id),
    CONSTRAINT station_operation_intervals_opened_by_event_fk
        FOREIGN KEY (opened_by_event_id) REFERENCES indicolors.station_operation_events (station_operation_event_id),
    CONSTRAINT station_operation_intervals_closed_by_event_fk
        FOREIGN KEY (closed_by_event_id) REFERENCES indicolors.station_operation_events (station_operation_event_id),
    CONSTRAINT station_operation_intervals_kind_check
        CHECK (interval_kind IN ('labor', 'pause', 'shift')),
    CONSTRAINT station_operation_intervals_duration_check
        CHECK (duration_ms IS NULL OR duration_ms >= 0),
    CONSTRAINT station_operation_intervals_open_consistency_check
        CHECK (is_open = FALSE OR (ended_at IS NULL AND closed_by_event_id IS NULL))
);

CREATE INDEX IF NOT EXISTS idx_station_operation_intervals_user_range
    ON indicolors.station_operation_intervals (company_id, user_id, started_at, ended_at);
CREATE INDEX IF NOT EXISTS idx_station_operation_intervals_order_process
    ON indicolors.station_operation_intervals (production_order_id, process_key, started_at)
    WHERE production_order_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_station_operation_intervals_catalog_item
    ON indicolors.station_operation_intervals (production_order_id, catalog_item_id, started_at)
    WHERE catalog_item_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_station_operation_intervals_open
    ON indicolors.station_operation_intervals (company_id, user_id, is_open)
    WHERE is_open = TRUE;

CREATE INDEX IF NOT EXISTS idx_station_operation_intervals_started_at_brin
    ON indicolors.station_operation_intervals USING BRIN (started_at);

COMMENT ON TABLE indicolors.station_operation_intervals IS 'Intervalos de labor/pausa/jornada materializados a partir de station_operation_events, para reportes de tiempo sin recalcular en cada request. Se puebla en la misma transacción del evento (servicio de aplicación) o vía trigger';

COMMENT ON COLUMN indicolors.station_operation_intervals.station_operation_interval_id IS
    'Identificador único del intervalo (UUID en texto)';
COMMENT ON COLUMN indicolors.station_operation_intervals.company_id IS 'Identificador de la empresa dueña del intervalo';
COMMENT ON COLUMN indicolors.station_operation_intervals.production_order_id IS 'Identificador de la Orden de Producción asociada; NULL en intervalos de jornada (shift)';
COMMENT ON COLUMN indicolors.station_operation_intervals.client_id IS 'Snapshot del cliente de la OP asociada';
COMMENT ON COLUMN indicolors.station_operation_intervals.user_id IS 'Operario dueño del intervalo';
COMMENT ON COLUMN indicolors.station_operation_intervals.process_key IS 'Clave del proceso o ítem al que pertenece el intervalo (ver process_key en station_operation_events)';
COMMENT ON COLUMN indicolors.station_operation_intervals.phase IS 'Etapa del wizard a la que pertenece el intervalo';
COMMENT ON COLUMN indicolors.station_operation_intervals.catalog_item_id IS 'Referencia lógica al catálogo maestro de Terminados/Acabados (mismo valor que production_order_postpress_lines.catalog_item_id); sin FK física, validar en el Service';
COMMENT ON COLUMN indicolors.station_operation_intervals.interval_kind IS 'Tipo de intervalo: labor (trabajo activo) | pause (paro) | shift (jornada)';
COMMENT ON COLUMN indicolors.station_operation_intervals.started_at IS 'Inicio del intervalo';
COMMENT ON COLUMN indicolors.station_operation_intervals.ended_at IS 'Fin del intervalo; NULL mientras is_open = TRUE';
COMMENT ON COLUMN indicolors.station_operation_intervals.duration_ms IS 'Duración en milisegundos, calculada al cerrar el intervalo';
COMMENT ON COLUMN indicolors.station_operation_intervals.pause_reason IS 'Motivo de la pausa; solo aplica cuando interval_kind = pause';
COMMENT ON COLUMN indicolors.station_operation_intervals.note IS 'Nota libre asociada al intervalo';
COMMENT ON COLUMN indicolors.station_operation_intervals.opened_by_event_id IS
    'Evento de station_operation_events que abrió el intervalo';
COMMENT ON COLUMN indicolors.station_operation_intervals.closed_by_event_id IS
    'Evento de station_operation_events que cerró el intervalo';
COMMENT ON COLUMN indicolors.station_operation_intervals.is_open IS 'True=el intervalo sigue abierto (sin ended_at ni closed_by_event_id)';
COMMENT ON COLUMN indicolors.station_operation_intervals.created_at IS 'Fecha y hora de creación del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.station_operation_intervals TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.station_operation_intervals TO indicolors_app;
