-- Turno de cada etapa cuando la orden está en proceso.
-- 1 es la siguiente en esa estación. No altera priority_rank de la orden completa.

CREATE TABLE IF NOT EXISTS indicolors.production_order_stage_priorities (
    stage_priority_id    CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id           CHARACTER VARYING(64)       NOT NULL,
    production_order_id  CHARACTER VARYING(64)       NOT NULL,
    stage_code           CHARACTER VARYING(32)       NOT NULL,
    priority_rank        INTEGER                     NOT NULL,
    created_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    created_by           CHARACTER VARYING(64),
    updated_by           CHARACTER VARYING(64),

    CONSTRAINT production_order_stage_priorities_pkey PRIMARY KEY (stage_priority_id),
    CONSTRAINT production_order_stage_priorities_order_stage_unique
        UNIQUE (company_id, production_order_id, stage_code),
    CONSTRAINT production_order_stage_priorities_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_stage_priorities_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_stage_priorities_rank_check CHECK (priority_rank > 0),
    CONSTRAINT production_order_stage_priorities_stage_check CHECK (stage_code IN (
        'preprensa',
        'corte-papel',
        'impresion',
        'terminados',
        'acabados'
    ))
);

CREATE INDEX IF NOT EXISTS idx_production_order_stage_priorities_queue
    ON indicolors.production_order_stage_priorities (company_id, stage_code, priority_rank);

COMMENT ON TABLE indicolors.production_order_stage_priorities IS 'Turno de trabajo por etapa de una orden en proceso. 1 es la siguiente en esa estación.';
COMMENT ON COLUMN indicolors.production_order_stage_priorities.stage_code IS 'Etapa: preprensa, corte-papel, impresion, terminados o acabados.';
COMMENT ON COLUMN indicolors.production_order_stage_priorities.priority_rank IS 'Posición en la cola de esa etapa. 1 es la siguiente.';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_stage_priorities TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_stage_priorities TO indicolors_app;
