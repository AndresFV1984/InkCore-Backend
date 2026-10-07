-- Tabla production_order_operators (módulo production-orders).

CREATE TABLE IF NOT EXISTS indicolors.production_order_operators (
    production_order_operator_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                   CHARACTER VARYING(64)       NOT NULL,
    production_order_id          CHARACTER VARYING(64)       NOT NULL,
    stage                        CHARACTER VARYING(32)       NOT NULL,  -- PREPRESS|CUTTING|PRINTING|FINISHED_PRODUCTS|FINISHING_PROCESSES|BILLING
    user_id                      CHARACTER VARYING(64)       NOT NULL,
    role_code                    CHARACTER VARYING(64),

    created_at                   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT production_order_operators_pkey PRIMARY KEY (production_order_operator_id),
    CONSTRAINT production_order_operators_order_stage_unique UNIQUE (production_order_id, stage),
    CONSTRAINT production_order_operators_company_fk FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_operators_order_fk FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_operators_user_fk FOREIGN KEY (user_id) REFERENCES indicolors.users (user_id),
    CONSTRAINT production_order_operators_stage_check CHECK (stage IN ('PREPRESS','CUTTING','PRINTING','FINISHED_PRODUCTS','FINISHING_PROCESSES','BILLING'))
);

CREATE INDEX IF NOT EXISTS idx_production_order_operators_company_id ON indicolors.production_order_operators (company_id);
CREATE INDEX IF NOT EXISTS idx_production_order_operators_order_id ON indicolors.production_order_operators (production_order_id);
CREATE INDEX IF NOT EXISTS idx_production_order_operators_user_id ON indicolors.production_order_operators (user_id);

COMMENT ON TABLE indicolors.production_order_operators IS 'Operador asignado por cada etapa de la OP; máximo 1 operador por etapa (UNIQUE order_id, stage)';

COMMENT ON COLUMN indicolors.production_order_operators.production_order_operator_id IS 'Identificador único del registro de operador por etapa';
COMMENT ON COLUMN indicolors.production_order_operators.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_operators.production_order_id IS 'Identificador de la Orden de Producción a la que pertenece el operador';
COMMENT ON COLUMN indicolors.production_order_operators.stage IS 'Etapa del wizard a la que corresponde el operador';
COMMENT ON COLUMN indicolors.production_order_operators.user_id IS 'Identificador del usuario/operador asignado a la etapa';
COMMENT ON COLUMN indicolors.production_order_operators.role_code IS 'Código de rol opcional del responsable (informativo; no obligatorio)';
COMMENT ON COLUMN indicolors.production_order_operators.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_operators.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_operators TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_operators TO indicolors_app;
