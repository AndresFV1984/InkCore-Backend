-- Tabla finishing_processes (formulario "Nueva operación de acabado") — un archivo por tabla.

CREATE TABLE IF NOT EXISTS indicolors.finishing_processes (
    finishing_process_id VARCHAR(64)    NOT NULL DEFAULT gen_random_uuid()::text,
    company_id           VARCHAR(64)    NOT NULL,
    name                 VARCHAR(150)   NOT NULL,
    min_cost             NUMERIC(12,2),
    value_per_cm2        NUMERIC(6,2)   NOT NULL DEFAULT 0,
    quick_access         BOOLEAN        NOT NULL DEFAULT FALSE,
    state                BOOLEAN        NOT NULL DEFAULT TRUE,
    creation_date        DATE           NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT finishing_processes_pkey PRIMARY KEY (finishing_process_id),
    CONSTRAINT finishing_processes_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT finishing_processes_name_company_unique
        UNIQUE (company_id, name),
    CONSTRAINT finishing_processes_min_cost_check
        CHECK (min_cost IS NULL OR min_cost >= 0),
    CONSTRAINT finishing_processes_value_per_cm2_check
        CHECK (value_per_cm2 >= 0 AND value_per_cm2 <= 9999)
);

CREATE INDEX IF NOT EXISTS idx_finishing_processes_company_id ON indicolors.finishing_processes (company_id);
CREATE INDEX IF NOT EXISTS idx_finishing_processes_name ON indicolors.finishing_processes (name);
CREATE INDEX IF NOT EXISTS idx_finishing_processes_state ON indicolors.finishing_processes (state);
CREATE INDEX IF NOT EXISTS idx_finishing_processes_company_state ON indicolors.finishing_processes (company_id, state);
CREATE INDEX IF NOT EXISTS idx_finishing_processes_quick_access ON indicolors.finishing_processes (quick_access);

COMMENT ON TABLE indicolors.finishing_processes IS 'Catálogo de Procesos de Acabado Aplicados (ej. Plegar, Embolsar), usados al configurar órdenes de producción';
COMMENT ON COLUMN indicolors.finishing_processes.finishing_process_id IS 'Identificador único del proceso de acabado aplicado';
COMMENT ON COLUMN indicolors.finishing_processes.company_id IS 'Identificador de la empresa dueña del proceso de acabado aplicado';
COMMENT ON COLUMN indicolors.finishing_processes.name IS 'Nombre del proceso de acabado aplicado (ej. Plegar, Embolsar)';
COMMENT ON COLUMN indicolors.finishing_processes.min_cost IS 'Costo mínimo del proceso de acabado aplicado; NULL si no aplica';
COMMENT ON COLUMN indicolors.finishing_processes.value_per_cm2 IS 'Valor por cm² del proceso de acabado aplicado; 0 si no aplica';
COMMENT ON COLUMN indicolors.finishing_processes.quick_access IS 'True=Muestra el proceso de acabado aplicado en la barra de selección rápida al configurar una orden de producción';
COMMENT ON COLUMN indicolors.finishing_processes.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.finishing_processes.creation_date IS 'Fecha de registro del proceso de acabado aplicado en el sistema';
