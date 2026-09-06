-- Agregado de cantidad disponible por OP (módulo Estación). Una fila por orden.

CREATE TABLE IF NOT EXISTS indicolors.station_order_progress (
    company_id            CHARACTER VARYING(64)       NOT NULL,
    production_order_id   CHARACTER VARYING(64)       NOT NULL,
    cantidad_disponible   INTEGER                     NOT NULL DEFAULT 0,
    updated_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT station_order_progress_pkey PRIMARY KEY (production_order_id),
    CONSTRAINT station_order_progress_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT station_order_progress_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT station_order_progress_cantidad_check
        CHECK (cantidad_disponible >= 0)
);

CREATE INDEX IF NOT EXISTS idx_station_order_progress_company
    ON indicolors.station_order_progress (company_id, cantidad_disponible);

COMMENT ON TABLE indicolors.station_order_progress IS
    'Agregado de cantidad disponible para entrega comercial por OP. cantidad_disponible = MIN(unidades procesadas por proceso real de la OP); no resta pedidos OPE. Se recalcula en la misma transacción que inserta avance_unidades.';

COMMENT ON COLUMN indicolors.station_order_progress.company_id IS 'Empresa dueña del registro (multi-tenant)';
COMMENT ON COLUMN indicolors.station_order_progress.production_order_id IS 'OP (PK; una fila por orden)';
COMMENT ON COLUMN indicolors.station_order_progress.cantidad_disponible IS 'Unidades disponibles para pedidos comerciales (≥ 0)';
COMMENT ON COLUMN indicolors.station_order_progress.updated_at IS 'Última actualización del agregado';

GRANT ALL PRIVILEGES ON TABLE indicolors.station_order_progress TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.station_order_progress TO indicolors_app;
