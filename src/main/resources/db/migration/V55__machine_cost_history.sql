CREATE TABLE indicolors.machine_cost_history (
    machine_cost_history_id    CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                 CHARACTER VARYING(64)       NOT NULL,
    machine_id                 CHARACTER VARYING(64)       NOT NULL,
    cost_per_hour              NUMERIC(12,2)                NOT NULL,
    purchase_cost               NUMERIC(14,2)                NOT NULL,
    annual_maintenance_cost     NUMERIC(14,2)                NOT NULL,
    monthly_operator_cost       NUMERIC(14,2)                NOT NULL,
    energy_cost_per_hour        NUMERIC(12,2)                NOT NULL,
    productive_hours_per_year   NUMERIC(10,2)                NOT NULL,
    effective_from              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    changed_by                  CHARACTER VARYING(64),
    CONSTRAINT machine_cost_history_pkey PRIMARY KEY (machine_cost_history_id),
    CONSTRAINT machine_cost_history_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT machine_cost_history_machine_fk
        FOREIGN KEY (machine_id) REFERENCES indicolors.machines (machine_id) ON DELETE CASCADE,
    CONSTRAINT machine_cost_history_changed_by_fk
        FOREIGN KEY (changed_by) REFERENCES indicolors.users (user_id)
);

CREATE INDEX idx_machine_cost_history_company_id ON indicolors.machine_cost_history (company_id);
CREATE INDEX idx_machine_cost_history_machine_id ON indicolors.machine_cost_history (machine_id, effective_from DESC);

COMMENT ON TABLE indicolors.machine_cost_history IS 'Auditoría append-only de cada recálculo de costo/hora de una máquina; se recomienda recalcular tarifas al menos cada trimestre o cuando cambie un costo significativo';
COMMENT ON COLUMN indicolors.machine_cost_history.machine_cost_history_id IS 'Identificador único del registro histórico';
COMMENT ON COLUMN indicolors.machine_cost_history.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.machine_cost_history.machine_id IS 'Identificador de la máquina (FK a machines)';
COMMENT ON COLUMN indicolors.machine_cost_history.cost_per_hour IS 'Snapshot del costo/hora resultante en ese momento';
COMMENT ON COLUMN indicolors.machine_cost_history.purchase_cost IS 'Snapshot del costo de adquisición en ese momento';
COMMENT ON COLUMN indicolors.machine_cost_history.annual_maintenance_cost IS 'Snapshot del mantenimiento anual en ese momento';
COMMENT ON COLUMN indicolors.machine_cost_history.monthly_operator_cost IS 'Snapshot del costo mensual del operario en ese momento';
COMMENT ON COLUMN indicolors.machine_cost_history.energy_cost_per_hour IS 'Snapshot del costo de energía por hora en ese momento';
COMMENT ON COLUMN indicolors.machine_cost_history.productive_hours_per_year IS 'Snapshot de las horas productivas al año en ese momento';
COMMENT ON COLUMN indicolors.machine_cost_history.effective_from IS 'Fecha/hora desde la que aplica este costo/hora';
COMMENT ON COLUMN indicolors.machine_cost_history.changed_by IS 'Usuario que generó el cambio (FK a users); NULL si fue un proceso automático';

GRANT ALL PRIVILEGES ON TABLE indicolors.machine_cost_history TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.machine_cost_history TO indicolors_app;

-- Trigger: cada vez que se crea una máquina o cambia cualquier componente de
-- su costo (y por lo tanto cost_per_hour, columna generada), se deja rastro
-- en machine_cost_history. No requiere que el backend se acuerde de hacerlo.
CREATE OR REPLACE FUNCTION indicolors.fn_log_machine_cost_history()
RETURNS TRIGGER AS $$
DECLARE
    v_changed_by CHARACTER VARYING(64);
    v_force TEXT;
BEGIN
    -- El backend fija estas variables de sesión (transaction-local) para
    -- atribuir el cambio y, en el recálculo trimestral, dejar auditoría
    -- aunque el costo/hora no haya variado.
    v_changed_by := NULLIF(current_setting('inkcore.changed_by', true), '');
    v_force := current_setting('inkcore.force_machine_cost_history', true);

    IF TG_OP = 'INSERT'
       OR NEW.cost_per_hour IS DISTINCT FROM OLD.cost_per_hour
       OR v_force = 'on'
    THEN
        INSERT INTO indicolors.machine_cost_history (
            company_id, machine_id, cost_per_hour, purchase_cost,
            annual_maintenance_cost, monthly_operator_cost,
            energy_cost_per_hour, productive_hours_per_year, changed_by
        )
        VALUES (
            NEW.company_id, NEW.machine_id, NEW.cost_per_hour, NEW.purchase_cost,
            NEW.annual_maintenance_cost, NEW.monthly_operator_cost,
            NEW.energy_cost_per_hour, NEW.productive_hours_per_year, v_changed_by
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_machines_log_cost_history ON indicolors.machines;
CREATE TRIGGER trg_machines_log_cost_history
    AFTER INSERT OR UPDATE ON indicolors.machines
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_log_machine_cost_history();

COMMENT ON FUNCTION indicolors.fn_log_machine_cost_history() IS
    'Inserta un snapshot en machine_cost_history al crear una máquina o cuando cambia su cost_per_hour calculado';
