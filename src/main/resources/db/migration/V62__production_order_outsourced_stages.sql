-- Etapas tercerizadas (brokered / outside work) por OP.
-- Cobro al cliente = sell_price (o vendor_cost × (1 + markup_percent/100)).
-- Ciclo operativo: PENDING → DISPATCHED → IN_PROGRESS → COMPLETED.

CREATE TABLE IF NOT EXISTS indicolors.production_order_outsourced_stages (
    production_order_outsourced_stage_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                           CHARACTER VARYING(64)       NOT NULL,
    production_order_id                  CHARACTER VARYING(64)       NOT NULL,
    stage                                CHARACTER VARYING(30)       NOT NULL,
    enabled                              BOOLEAN                     NOT NULL DEFAULT TRUE,
    vendor_cost                          NUMERIC(14,2)               NOT NULL DEFAULT 0,
    markup_percent                       NUMERIC(8,2)                NOT NULL DEFAULT 0,
    sell_price                           NUMERIC(14,2)               NOT NULL DEFAULT 0,
    supplier_id                          CHARACTER VARYING(64),
    supplier_name                        CHARACTER VARYING(200),

    -- Cotización / acuerdo
    quoted_at                            DATE,
    quantity                             INTEGER                     NOT NULL DEFAULT 0,
    cost_responsible_user_id             CHARACTER VARYING(64),
    cost_responsible_user_name           CHARACTER VARYING(200),
    quote_valid_until                    DATE,

    -- Envío al proveedor
    sent_at                              DATE,
    sent_description                     CHARACTER VARYING(1000),
    expected_return_at                   DATE,

    -- Recepción / cierre operativo
    received_at                          DATE,
    received_by_user_id                  CHARACTER VARYING(64),
    fulfillment_status                   CHARACTER VARYING(20)       NOT NULL DEFAULT 'PENDING',

    created_at                           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_outsourced_stages_pkey PRIMARY KEY (production_order_outsourced_stage_id),
    CONSTRAINT production_order_outsourced_stages_order_stage_unique UNIQUE (production_order_id, stage),
    CONSTRAINT production_order_outsourced_stages_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_outsourced_stages_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_outsourced_stages_stage_check CHECK (
        stage IN ('PREPRESS', 'CUTTING', 'PRINTING', 'FINISHED_PRODUCTS', 'FINISHING_PROCESSES')
    ),
    CONSTRAINT production_order_outsourced_stages_vendor_cost_check CHECK (vendor_cost >= 0),
    CONSTRAINT production_order_outsourced_stages_markup_check CHECK (markup_percent >= 0),
    CONSTRAINT production_order_outsourced_stages_sell_price_check CHECK (sell_price >= 0),
    CONSTRAINT production_order_outsourced_stages_quantity_check CHECK (quantity >= 0),
    CONSTRAINT production_order_outsourced_stages_fulfillment_check CHECK (
        fulfillment_status IN ('PENDING', 'DISPATCHED', 'IN_PROGRESS', 'COMPLETED')
    )
);

CREATE INDEX IF NOT EXISTS idx_production_order_outsourced_stages_company_id
    ON indicolors.production_order_outsourced_stages (company_id);
CREATE INDEX IF NOT EXISTS idx_production_order_outsourced_stages_order_id
    ON indicolors.production_order_outsourced_stages (production_order_id);
CREATE INDEX IF NOT EXISTS idx_production_order_outsourced_stages_fulfillment
    ON indicolors.production_order_outsourced_stages (company_id, fulfillment_status)
    WHERE enabled = TRUE;

COMMENT ON TABLE indicolors.production_order_outsourced_stages IS
    'Etapa ejecutada por proveedor externo; cobro al cliente = sell_price (costo × markup). Ciclo PENDING→DISPATCHED→IN_PROGRESS→COMPLETED. Máximo 1 por etapa.';
COMMENT ON COLUMN indicolors.production_order_outsourced_stages.stage IS
    'PREPRESS|CUTTING|PRINTING|FINISHED_PRODUCTS|FINISHING_PROCESSES';
COMMENT ON COLUMN indicolors.production_order_outsourced_stages.vendor_cost IS
    'Costo pactado con el proveedor (COP)';
COMMENT ON COLUMN indicolors.production_order_outsourced_stages.markup_percent IS
    'Markup % sobre vendor_cost';
COMMENT ON COLUMN indicolors.production_order_outsourced_stages.sell_price IS
    'Cobro al cliente de la etapa; si enabled, sustituye el cobro interno en totalToCharge';
COMMENT ON COLUMN indicolors.production_order_outsourced_stages.supplier_id IS
    'Id del proveedor en directorio (opcional)';
COMMENT ON COLUMN indicolors.production_order_outsourced_stages.quoted_at IS
    'Fecha del acuerdo/registro de cotización con el proveedor';
COMMENT ON COLUMN indicolors.production_order_outsourced_stages.quantity IS
    'Cantidad a tercerizar (unidades de la etapa)';
COMMENT ON COLUMN indicolors.production_order_outsourced_stages.cost_responsible_user_id IS
    'Usuario de la litografía que autoriza/gestiona el costo con el proveedor';
COMMENT ON COLUMN indicolors.production_order_outsourced_stages.quote_valid_until IS
    'Fecha límite de vigencia de la cotización del proveedor';
COMMENT ON COLUMN indicolors.production_order_outsourced_stages.sent_at IS
    'Fecha de envío/entrega del trabajo al proveedor';
COMMENT ON COLUMN indicolors.production_order_outsourced_stages.sent_description IS
    'Qué se entrega de la etapa al proveedor';
COMMENT ON COLUMN indicolors.production_order_outsourced_stages.expected_return_at IS
    'Fecha límite esperada de retorno del proveedor';
COMMENT ON COLUMN indicolors.production_order_outsourced_stages.received_at IS
    'Fecha en que producción/admin registra la recepción del trabajo';
COMMENT ON COLUMN indicolors.production_order_outsourced_stages.received_by_user_id IS
    'Usuario (producción/admin) que registró la recepción';
COMMENT ON COLUMN indicolors.production_order_outsourced_stages.fulfillment_status IS
    'PENDING=pendiente; DISPATCHED=envío proveedor; IN_PROGRESS=en proceso proveedor; COMPLETED=finalizado proveedor';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_outsourced_stages TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_outsourced_stages TO indicolors_app;
