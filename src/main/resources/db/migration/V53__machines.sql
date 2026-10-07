CREATE TABLE indicolors.machines (
    machine_id                 CHARACTER VARYING(64)  NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                 CHARACTER VARYING(64)  NOT NULL,
    name                       CHARACTER VARYING(150) NOT NULL,
    machine_type               CHARACTER VARYING(20)  NOT NULL,
    manufacturer               CHARACTER VARYING(150),
    model                      CHARACTER VARYING(150),
    purchase_cost              NUMERIC(14,2)           NOT NULL,
    useful_life_years          NUMERIC(5,2)            NOT NULL,
    annual_maintenance_cost    NUMERIC(14,2)           NOT NULL DEFAULT 0,
    monthly_operator_cost      NUMERIC(14,2)           NOT NULL DEFAULT 0,
    energy_cost_per_hour       NUMERIC(12,2)           NOT NULL DEFAULT 0,
    productive_hours_per_year  NUMERIC(10,2)           NOT NULL,
    cost_per_hour              NUMERIC(12,2) GENERATED ALWAYS AS (
        (
            (purchase_cost / useful_life_years)
            + annual_maintenance_cost
            + (monthly_operator_cost * 12)
        ) / productive_hours_per_year
        + energy_cost_per_hour
    ) STORED,
    state                      BOOLEAN                 NOT NULL DEFAULT TRUE,
    creation_date              DATE                    NOT NULL DEFAULT CURRENT_DATE,
    updated_at                 TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT machines_pkey PRIMARY KEY (machine_id),
    CONSTRAINT machines_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT machines_name_company_unique UNIQUE (company_id, name),
    CONSTRAINT machines_machine_type_check
        CHECK (machine_type IN ('preprensa','corte-papel','impresion','terminados','acabados')),
    CONSTRAINT machines_purchase_cost_check CHECK (purchase_cost >= 0),
    CONSTRAINT machines_useful_life_years_check CHECK (useful_life_years > 0),
    CONSTRAINT machines_annual_maintenance_cost_check CHECK (annual_maintenance_cost >= 0),
    CONSTRAINT machines_monthly_operator_cost_check CHECK (monthly_operator_cost >= 0),
    CONSTRAINT machines_energy_cost_per_hour_check CHECK (energy_cost_per_hour >= 0),
    CONSTRAINT machines_productive_hours_per_year_check CHECK (productive_hours_per_year > 0)
);

CREATE INDEX idx_machines_company_id ON indicolors.machines (company_id);
CREATE INDEX idx_machines_machine_type ON indicolors.machines (machine_type);
CREATE INDEX idx_machines_state ON indicolors.machines (state);
CREATE INDEX idx_machines_company_state ON indicolors.machines (company_id, state);
CREATE INDEX idx_machines_company_type_state ON indicolors.machines (company_id, machine_type, state);

COMMENT ON TABLE indicolors.machines IS 'Catálogo de máquinas por compañía, con costo/hora calculado (depreciación + mantenimiento + operario + energía, sobre horas productivas reales), usado para costear cada fase de una Orden de Producción';
COMMENT ON COLUMN indicolors.machines.machine_id IS 'Identificador único de la máquina';
COMMENT ON COLUMN indicolors.machines.company_id IS 'Identificador de la empresa dueña de la máquina';
COMMENT ON COLUMN indicolors.machines.name IS 'Nombre/identificador de la máquina (ej. Offset Heidelberg 4 colores)';
COMMENT ON COLUMN indicolors.machines.machine_type IS 'Fase de la Orden de Producción a la que pertenece: preprensa|corte-papel|impresion|terminados|acabados (mismo dominio que station_operation_events.phase)';
COMMENT ON COLUMN indicolors.machines.manufacturer IS 'Fabricante de la máquina (opcional)';
COMMENT ON COLUMN indicolors.machines.model IS 'Modelo de la máquina (opcional)';
COMMENT ON COLUMN indicolors.machines.purchase_cost IS 'Costo de adquisición de la máquina';
COMMENT ON COLUMN indicolors.machines.useful_life_years IS 'Vida útil estimada en años, usada para el componente de depreciación anual';
COMMENT ON COLUMN indicolors.machines.annual_maintenance_cost IS 'Costo de mantenimiento estimado al año';
COMMENT ON COLUMN indicolors.machines.monthly_operator_cost IS 'Costo mensual del operario dedicado a esta máquina; puede ser 0 si el operario ya se costea en otro rubro';
COMMENT ON COLUMN indicolors.machines.energy_cost_per_hour IS 'Costo de energía/consumo eléctrico por hora de operación';
COMMENT ON COLUMN indicolors.machines.productive_hours_per_year IS 'Horas REALES de producción al año (descontando mantenimiento, montajes y tiempos muertos); NO son horas de calendario laboral. Usar este dato mal es el error más común del sector y subestima el costo real';
COMMENT ON COLUMN indicolors.machines.cost_per_hour IS 'Costo/hora calculado automáticamente: ((costo de compra / vida útil) + mantenimiento anual + (costo mensual operario * 12)) / horas productivas al año + energía por hora';
COMMENT ON COLUMN indicolors.machines.state IS 'True=Activa, False=Inactiva';
COMMENT ON COLUMN indicolors.machines.creation_date IS 'Fecha de registro de la máquina en el sistema';
COMMENT ON COLUMN indicolors.machines.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.machines TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.machines TO indicolors_app;
