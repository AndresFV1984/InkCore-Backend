-- Tabla production_orders (módulo production-orders).

CREATE TABLE IF NOT EXISTS indicolors.production_orders (
    production_order_id              CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                       CHARACTER VARYING(64)       NOT NULL,
    order_number                     CHARACTER VARYING(20)       NOT NULL,
    version                          BIGINT                      NOT NULL DEFAULT 0,

    -- Especificaciones
    client_id                        CHARACTER VARYING(64)       NOT NULL,
    work_name                        CHARACTER VARYING(150)      NOT NULL,
    seller_id                        CHARACTER VARYING(64),
    order_date                       DATE                        NOT NULL DEFAULT CURRENT_DATE,
    requested_quantity               INTEGER                     NOT NULL,
    proposal_quantity_1              INTEGER,
    proposal_quantity_2              INTEGER,
    specifications_completed_at      TIMESTAMP WITHOUT TIME ZONE,

    -- Progreso de pasos que no tienen tabla 1:1 propia
    cutting_completed_at             TIMESTAMP WITHOUT TIME ZONE,
    printing_completed_at            TIMESTAMP WITHOUT TIME ZONE,
    finished_products_completed_at   TIMESTAMP WITHOUT TIME ZONE,
    finishing_processes_completed_at TIMESTAMP WITHOUT TIME ZONE,

    -- Corte de papel (globales del paso, pocos campos, se quedan en el núcleo)
    client_supplies_paper_default    BOOLEAN,
    rounding_margin                  INTEGER                     DEFAULT 2,

    status                           CHARACTER VARYING(30)       NOT NULL DEFAULT 'PENDING',
    state                            BOOLEAN                     NOT NULL DEFAULT TRUE,
    created_at                       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    created_by                       CHARACTER VARYING(64),
    updated_by                       CHARACTER VARYING(64),

    CONSTRAINT production_orders_pkey PRIMARY KEY (production_order_id),
    CONSTRAINT production_orders_order_number_company_unique UNIQUE (company_id, order_number),
    CONSTRAINT production_orders_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_orders_client_fk FOREIGN KEY (client_id) REFERENCES indicolors.clients (client_id),
    CONSTRAINT production_orders_seller_fk FOREIGN KEY (seller_id) REFERENCES indicolors.sellers (seller_id),
    CONSTRAINT production_orders_requested_quantity_check CHECK (requested_quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_production_orders_company_id ON indicolors.production_orders (company_id);
CREATE INDEX IF NOT EXISTS idx_production_orders_client_id ON indicolors.production_orders (client_id);
CREATE INDEX IF NOT EXISTS idx_production_orders_status ON indicolors.production_orders (status);
CREATE INDEX IF NOT EXISTS idx_production_orders_company_state ON indicolors.production_orders (company_id, state);
CREATE INDEX IF NOT EXISTS idx_production_orders_seller_id ON indicolors.production_orders (seller_id);

COMMENT ON TABLE indicolors.production_orders IS 'Núcleo de la Orden de Producción: Especificaciones y control de flujo. Preprensa y Cobro viven en tablas 1:1 separadas por tamaño (production_order_prepress_details, production_order_billing_details)';

COMMENT ON COLUMN indicolors.production_orders.production_order_id IS 'Identificador único de la Orden de Producción';
COMMENT ON COLUMN indicolors.production_orders.company_id IS 'Identificador de la empresa dueña de la Orden de Producción';
COMMENT ON COLUMN indicolors.production_orders.order_number IS 'Consecutivo corto de la OP por compañía (ej. OP-1, OP-42), generado por el backend; único por compañía';
COMMENT ON COLUMN indicolors.production_orders.version IS 'Control de concurrencia optimista';
COMMENT ON COLUMN indicolors.production_orders.client_id IS 'Identificador del cliente para el que se genera la Orden de Producción';
COMMENT ON COLUMN indicolors.production_orders.work_name IS 'Nombre del trabajo o pieza a producir';
COMMENT ON COLUMN indicolors.production_orders.seller_id IS 'Identificador del vendedor asociado a la Orden de Producción';
COMMENT ON COLUMN indicolors.production_orders.order_date IS 'Fecha de creación/registro de la Orden de Producción';
COMMENT ON COLUMN indicolors.production_orders.requested_quantity IS 'Cantidad solicitada por el cliente';
COMMENT ON COLUMN indicolors.production_orders.proposal_quantity_1 IS 'Cantidad alterna de propuesta 1 para comparativo de costeo (opcional)';
COMMENT ON COLUMN indicolors.production_orders.proposal_quantity_2 IS 'Cantidad alterna de propuesta 2 para comparativo de costeo (opcional)';
COMMENT ON COLUMN indicolors.production_orders.specifications_completed_at IS 'Fecha/hora en que se completó el paso de Especificaciones';
COMMENT ON COLUMN indicolors.production_orders.cutting_completed_at IS 'Fecha/hora en que se completó el paso de Corte de papel';
COMMENT ON COLUMN indicolors.production_orders.printing_completed_at IS 'Fecha/hora en que se completó el paso de Impresión';
COMMENT ON COLUMN indicolors.production_orders.finished_products_completed_at IS 'Fecha/hora en que se completó el paso de Terminados';
COMMENT ON COLUMN indicolors.production_orders.finishing_processes_completed_at IS 'Fecha/hora en que se completó el paso de Acabados';
COMMENT ON COLUMN indicolors.production_orders.client_supplies_paper_default IS 'Valor por defecto del paso Corte de papel: True=el cliente suministra el papel';
COMMENT ON COLUMN indicolors.production_orders.rounding_margin IS 'Margen de redondeo aplicado en los cálculos del paso de Corte de papel';
COMMENT ON COLUMN indicolors.production_orders.status IS 'Estado de la OP en planta: PENDING, IN_PROGRESS, etc.';
COMMENT ON COLUMN indicolors.production_orders.state IS 'True=Activa, False=Borrador eliminado (baja lógica)';
COMMENT ON COLUMN indicolors.production_orders.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_orders.updated_at IS 'Fecha y hora de la última actualización del registro';
COMMENT ON COLUMN indicolors.production_orders.created_by IS 'Identificador del usuario que creó la Orden de Producción';
COMMENT ON COLUMN indicolors.production_orders.updated_by IS 'Identificador del usuario que hizo la última actualización';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_orders TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_orders TO indicolors_app;
