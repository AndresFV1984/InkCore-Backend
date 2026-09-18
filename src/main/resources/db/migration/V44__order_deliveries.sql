-- Entregas comerciales append-only (módulo Pedidos) + reversiones.

CREATE TABLE IF NOT EXISTS indicolors.order_deliveries (
    order_delivery_id     CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id            CHARACTER VARYING(64)       NOT NULL,
    delivery_number       CHARACTER VARYING(32)       NOT NULL,
    production_order_id   CHARACTER VARYING(64)       NOT NULL,
    client_id             CHARACTER VARYING(64)       NOT NULL,
    seller_id             CHARACTER VARYING(64),

    movement_type         CHARACTER VARYING(16)       NOT NULL DEFAULT 'entrega',
    delivery_type         CHARACTER VARYING(16)       NOT NULL,
    reversed_delivery_id  CHARACTER VARYING(64),
    quantity_delivered    INTEGER                     NOT NULL,
    unit_price            NUMERIC(12,2)               NOT NULL DEFAULT 0,
    total_value           NUMERIC(14,2)               NOT NULL DEFAULT 0,
    available_before      INTEGER                     NOT NULL DEFAULT 0,

    work_name_snapshot    CHARACTER VARYING(150),
    client_name_snapshot  CHARACTER VARYING(200),

    delivered_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    delivered_by          CHARACTER VARYING(64)       NOT NULL,
    notes                 TEXT,

    created_at            TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT order_deliveries_pkey PRIMARY KEY (order_delivery_id),
    CONSTRAINT order_deliveries_delivery_number_company_unique UNIQUE (company_id, delivery_number),
    CONSTRAINT order_deliveries_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT order_deliveries_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id),
    CONSTRAINT order_deliveries_client_fk
        FOREIGN KEY (client_id) REFERENCES indicolors.clients (client_id),
    CONSTRAINT order_deliveries_seller_fk
        FOREIGN KEY (seller_id) REFERENCES indicolors.sellers (seller_id),
    CONSTRAINT order_deliveries_delivered_by_fk
        FOREIGN KEY (delivered_by) REFERENCES indicolors.users (user_id),
    CONSTRAINT order_deliveries_reversed_fk
        FOREIGN KEY (reversed_delivery_id) REFERENCES indicolors.order_deliveries (order_delivery_id),
    CONSTRAINT order_deliveries_type_check
        CHECK (delivery_type IN ('parcial', 'total')),
    CONSTRAINT order_deliveries_movement_type_check
        CHECK (movement_type IN ('entrega', 'reversion')),
    CONSTRAINT order_deliveries_reversion_ref_check
        CHECK (movement_type <> 'reversion' OR reversed_delivery_id IS NOT NULL),
    CONSTRAINT order_deliveries_entrega_no_ref_check
        CHECK (movement_type <> 'entrega' OR reversed_delivery_id IS NULL),
    CONSTRAINT order_deliveries_quantity_check
        CHECK (quantity_delivered > 0),
    CONSTRAINT order_deliveries_unit_price_check
        CHECK (unit_price >= 0),
    CONSTRAINT order_deliveries_available_before_check
        CHECK (available_before >= 0),
    CONSTRAINT order_deliveries_delivery_number_format_check
        CHECK (delivery_number ~ '^ODP-[0-9]+$')
);

CREATE INDEX IF NOT EXISTS idx_order_deliveries_company_id ON indicolors.order_deliveries (company_id);
CREATE INDEX IF NOT EXISTS idx_order_deliveries_order_time ON indicolors.order_deliveries (company_id, production_order_id, delivered_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_deliveries_client_time ON indicolors.order_deliveries (company_id, client_id, delivered_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_deliveries_seller_time ON indicolors.order_deliveries (company_id, seller_id, delivered_at DESC)
    WHERE seller_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_order_deliveries_delivery_number ON indicolors.order_deliveries (company_id, delivery_number);
CREATE UNIQUE INDEX IF NOT EXISTS uq_order_deliveries_reversed_once
    ON indicolors.order_deliveries (company_id, reversed_delivery_id)
    WHERE reversed_delivery_id IS NOT NULL;

COMMENT ON TABLE indicolors.order_deliveries IS 'Bitácora append-only de entregas comerciales (parciales/totales) y sus reversiones (movement_type=reversion) de una OP a su cliente/representante. No admite UPDATE ni DELETE en producción; correcciones = nueva fila (reversión) con notes explicativa';

COMMENT ON COLUMN indicolors.order_deliveries.order_delivery_id IS 'Identificador único de la entrega o de su reversión';
COMMENT ON COLUMN indicolors.order_deliveries.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.order_deliveries.delivery_number IS 'Consecutivo corto del pedido/entrega por compañía (ej. ODP-1, ODP-42), generado por el backend; único por compañía; cada reversión también recibe el suyo propio';
COMMENT ON COLUMN indicolors.order_deliveries.production_order_id IS 'Identificador de la Orden de Producción entregada';
COMMENT ON COLUMN indicolors.order_deliveries.client_id IS 'Cliente que recibe la entrega (snapshot desde production_orders.client_id)';
COMMENT ON COLUMN indicolors.order_deliveries.seller_id IS 'Representante/vendedor asociado a la entrega, si aplica';
COMMENT ON COLUMN indicolors.order_deliveries.movement_type IS 'entrega=movimiento normal que suma disponibilidad entregada | reversion=anulación de una entrega previa que la resta (append-only, nunca UPDATE/DELETE)';
COMMENT ON COLUMN indicolors.order_deliveries.delivery_type IS 'parcial=entrega parcial de unidades | total=entrega final que cierra la OP comercialmente; en una reversión describe el subtipo de la entrega original que se anula';
COMMENT ON COLUMN indicolors.order_deliveries.reversed_delivery_id IS 'order_delivery_id de la entrega original que se anula; obligatorio si movement_type=reversion, y solo puede apuntar a una fila con movement_type=entrega que aún no haya sido revertida';
COMMENT ON COLUMN indicolors.order_deliveries.quantity_delivered IS 'Unidades del movimiento (> 0); en una reversión es la misma cantidad de la entrega original que se está anulando, el trigger la resta';
COMMENT ON COLUMN indicolors.order_deliveries.unit_price IS 'Precio unitario snapshot usado para valorizar esta entrega';
COMMENT ON COLUMN indicolors.order_deliveries.total_value IS 'quantity_delivered * unit_price, calculado por el backend al insertar; en una reversión debe igualar el total_value de la entrega original';
COMMENT ON COLUMN indicolors.order_deliveries.available_before IS 'Unidades disponibles para entrega justo antes de este movimiento; lo calcula el trigger de validación, no lo envía el cliente';
COMMENT ON COLUMN indicolors.order_deliveries.work_name_snapshot IS 'Snapshot de production_orders.work_name al momento de insertar';
COMMENT ON COLUMN indicolors.order_deliveries.client_name_snapshot IS 'Snapshot de clients.name al momento de insertar';
COMMENT ON COLUMN indicolors.order_deliveries.delivered_at IS 'Fecha/hora real de la entrega (puede venir del cliente)';
COMMENT ON COLUMN indicolors.order_deliveries.delivered_by IS 'Usuario que registró la entrega o la reversión';
COMMENT ON COLUMN indicolors.order_deliveries.notes IS 'Nota libre; usada también para explicar anulaciones';
COMMENT ON COLUMN indicolors.order_deliveries.created_at IS 'Fecha y hora de persistencia del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.order_deliveries TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.order_deliveries TO indicolors_app;
