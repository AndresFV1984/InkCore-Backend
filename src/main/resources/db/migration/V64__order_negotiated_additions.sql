-- Adicionales negociados: cambios de alcance que el cliente aprueba y paga.
-- Un ADN por adicional. Anular marca voided_at en la misma fila (no asigna otro consecutivo).

CREATE TABLE IF NOT EXISTS indicolors.order_negotiated_additions (
    order_negotiated_addition_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                   CHARACTER VARYING(64)       NOT NULL,
    addition_number              CHARACTER VARYING(32)       NOT NULL,
    production_order_id          CHARACTER VARYING(64)       NOT NULL,
    client_id                    CHARACTER VARYING(64)       NOT NULL,

    movement_type                CHARACTER VARYING(16)       NOT NULL DEFAULT 'registro',
    concept                      CHARACTER VARYING(200)      NOT NULL,
    change_reason                CHARACTER VARYING(32)       NOT NULL,
    amount                       NUMERIC(14,2)               NOT NULL,
    schedule_impact_days         INTEGER                     NOT NULL DEFAULT 0,
    approved_by_client_name      CHARACTER VARYING(150)      NOT NULL,
    approval_channel             CHARACTER VARYING(32)       NOT NULL,
    approval_reference           CHARACTER VARYING(120),
    approved_at                  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    reversed_addition_id         CHARACTER VARYING(64),

    registered_by                CHARACTER VARYING(64)       NOT NULL,
    notes                        TEXT,
    created_at                   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    voided_at                    TIMESTAMP WITHOUT TIME ZONE,
    voided_by                    CHARACTER VARYING(64),

    CONSTRAINT order_negotiated_additions_pkey PRIMARY KEY (order_negotiated_addition_id),
    CONSTRAINT order_negotiated_additions_number_company_unique UNIQUE (company_id, addition_number),
    CONSTRAINT order_negotiated_additions_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT order_negotiated_additions_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id),
    CONSTRAINT order_negotiated_additions_client_fk
        FOREIGN KEY (client_id) REFERENCES indicolors.clients (client_id),
    CONSTRAINT order_negotiated_additions_registered_by_fk
        FOREIGN KEY (registered_by) REFERENCES indicolors.users (user_id),
    CONSTRAINT order_negotiated_additions_reversed_fk
        FOREIGN KEY (reversed_addition_id) REFERENCES indicolors.order_negotiated_additions (order_negotiated_addition_id),
    CONSTRAINT order_negotiated_additions_voided_by_fk
        FOREIGN KEY (voided_by) REFERENCES indicolors.users (user_id),
    CONSTRAINT order_negotiated_additions_type_check
        CHECK (movement_type IN ('registro', 'anulacion')),
    CONSTRAINT order_negotiated_additions_amount_check
        CHECK (amount > 0),
    CONSTRAINT order_negotiated_additions_concept_check
        CHECK (char_length(btrim(concept)) > 0),
    CONSTRAINT order_negotiated_additions_approver_check
        CHECK (char_length(btrim(approved_by_client_name)) > 0),
    CONSTRAINT order_negotiated_additions_reason_check
        CHECK (change_reason IN ('solicitud_cliente', 'condicion_no_prevista', 'ajuste_planta', 'otro')),
    CONSTRAINT order_negotiated_additions_channel_check
        CHECK (approval_channel IN ('correo', 'whatsapp', 'firma', 'otro')),
    CONSTRAINT order_negotiated_additions_schedule_days_check
        CHECK (schedule_impact_days >= 0),
    CONSTRAINT order_negotiated_additions_reversion_ref_check
        CHECK (movement_type <> 'anulacion' OR reversed_addition_id IS NOT NULL),
    CONSTRAINT order_negotiated_additions_non_reversion_no_ref_check
        CHECK (movement_type = 'anulacion' OR reversed_addition_id IS NULL),
    CONSTRAINT order_negotiated_additions_number_format_check
        CHECK (addition_number ~ '^ADN-[0-9]+$')
);

CREATE INDEX IF NOT EXISTS idx_order_negotiated_additions_company_id
    ON indicolors.order_negotiated_additions (company_id);
CREATE INDEX IF NOT EXISTS idx_order_negotiated_additions_order_time
    ON indicolors.order_negotiated_additions (company_id, production_order_id, approved_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uq_order_negotiated_additions_reversed_once
    ON indicolors.order_negotiated_additions (company_id, reversed_addition_id)
    WHERE reversed_addition_id IS NOT NULL;

COMMENT ON TABLE indicolors.order_negotiated_additions IS
    'Cambios de alcance negociados con el cliente. Un ADN por adicional. Anular llena voided_at en la misma fila y no asigna otro consecutivo.';
COMMENT ON COLUMN indicolors.order_negotiated_additions.order_negotiated_addition_id IS 'Identificador único del movimiento';
COMMENT ON COLUMN indicolors.order_negotiated_additions.company_id IS 'Empresa dueña del registro';
COMMENT ON COLUMN indicolors.order_negotiated_additions.addition_number IS 'Consecutivo ADN-{n} por compañía';
COMMENT ON COLUMN indicolors.order_negotiated_additions.production_order_id IS 'OP a la que se suma el adicional';
COMMENT ON COLUMN indicolors.order_negotiated_additions.client_id IS 'Cliente snapshot desde production_orders.client_id';
COMMENT ON COLUMN indicolors.order_negotiated_additions.movement_type IS 'registro = alcance aprobado | anulacion = legado (preferir voided_at)';
COMMENT ON COLUMN indicolors.order_negotiated_additions.concept IS 'Qué cambió de alcance (lo que el cliente aprueba)';
COMMENT ON COLUMN indicolors.order_negotiated_additions.change_reason IS
    'Por qué cambió el alcance: solicitud_cliente | condicion_no_prevista | ajuste_planta | otro';
COMMENT ON COLUMN indicolors.order_negotiated_additions.amount IS 'Monto que el cliente paga por ese cambio; siempre positivo';
COMMENT ON COLUMN indicolors.order_negotiated_additions.schedule_impact_days IS
    'Días que se suman a la entrega. 0 si el plazo no cambia';
COMMENT ON COLUMN indicolors.order_negotiated_additions.approved_by_client_name IS 'Persona del cliente que aprobó el cambio';
COMMENT ON COLUMN indicolors.order_negotiated_additions.approval_channel IS
    'Medio escrito de la aprobación: correo | whatsapp | firma | otro';
COMMENT ON COLUMN indicolors.order_negotiated_additions.approval_reference IS
    'Referencia del mensaje, correo o documento con el que el cliente aprobó';
COMMENT ON COLUMN indicolors.order_negotiated_additions.approved_at IS 'Fecha/hora en que el cliente aprobó';
COMMENT ON COLUMN indicolors.order_negotiated_additions.reversed_addition_id IS 'Movimiento registro que se anula (legado; preferir voided_at)';
COMMENT ON COLUMN indicolors.order_negotiated_additions.registered_by IS 'Usuario que registró el movimiento';
COMMENT ON COLUMN indicolors.order_negotiated_additions.notes IS 'Nota libre';
COMMENT ON COLUMN indicolors.order_negotiated_additions.created_at IS 'Fecha y hora de persistencia';
COMMENT ON COLUMN indicolors.order_negotiated_additions.voided_at IS
    'Fecha en que se anuló este ADN. NULL = vigente y suma a lo que el cliente paga.';
COMMENT ON COLUMN indicolors.order_negotiated_additions.voided_by IS
    'Usuario que anuló el adicional';

GRANT ALL PRIVILEGES ON TABLE indicolors.order_negotiated_additions TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.order_negotiated_additions TO indicolors_app;
