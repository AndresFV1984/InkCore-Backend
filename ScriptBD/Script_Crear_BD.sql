-- ============================================================================
-- MIGRACIÓN: COSTEO POR HORA-MÁQUINA Y CONTROL DE MERMAS/DESPERDICIOS
-- Continuación de Script_Crear_BD.sql. Se ejecuta DESPUÉS del script base
-- completo (requiere que existan: companies, users, production_orders,
-- production_order_paper_rows, production_order_prepress_details,
-- production_order_postpress_records, production_order_postpress_lines,
-- station_operation_events).
--
-- No modifica ni reordena nada del script original. Sigue exactamente las
-- mismas convenciones: ids CHARACTER VARYING(64) con gen_random_uuid()::text,
-- company_id en toda tabla, TIMESTAMP WITHOUT TIME ZONE, CONSTRAINT con
-- nombre explícito, índices explícitos, COMMENT ON TABLE/COLUMN, GRANT
-- explícito por tabla a indicolors_owner / indicolors_app.
--
-- Objetivo: dejar de costear las Órdenes de Producción solo por cantidad
-- solicitada + material, incorporando (1) el costo real de tener la
-- máquina prendida en cada fase y (2) las mermas/desperdicios valorizados,
-- para poder comparar costo cotizado vs. costo real y conocer la ganancia
-- neta real de cada OP.
-- ============================================================================

\connect inkcore;

-- ============================================================================
-- 50. CATÁLOGO DE MÁQUINAS (formulario "Nueva máquina")
-- ============================================================================
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

-- ============================================================================
-- 50.1 HISTORIAL DE COSTO/HORA POR MÁQUINA (auditoría de recálculo de tarifas)
-- ============================================================================
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

-- ============================================================================
-- 50.2 USO DE MÁQUINA POR FASE DE LA ORDEN (Preprensa/Corte/Impresión/Terminados/Acabados)
-- ============================================================================
CREATE TABLE indicolors.production_order_machine_usage (
    production_order_machine_usage_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                        CHARACTER VARYING(64)       NOT NULL,
    production_order_id               CHARACTER VARYING(64)       NOT NULL,
    phase                              CHARACTER VARYING(20)       NOT NULL,
    machine_id                        CHARACTER VARYING(64)       NOT NULL,
    machine_name_snapshot             CHARACTER VARYING(150)      NOT NULL,
    cost_per_hour_snapshot            NUMERIC(12,2)               NOT NULL,
    estimated_setup_minutes           INTEGER                     NOT NULL DEFAULT 0,
    estimated_run_minutes             INTEGER                     NOT NULL DEFAULT 0,
    estimated_machine_cost            NUMERIC(12,2) GENERATED ALWAYS AS (
        ((estimated_setup_minutes + estimated_run_minutes) / 60.0) * cost_per_hour_snapshot
    ) STORED,
    actual_setup_minutes              INTEGER,
    actual_run_minutes                INTEGER,
    actual_machine_cost               NUMERIC(12,2) GENERATED ALWAYS AS (
        CASE
            WHEN actual_setup_minutes IS NULL AND actual_run_minutes IS NULL THEN NULL
            ELSE ((COALESCE(actual_setup_minutes, 0) + COALESCE(actual_run_minutes, 0)) / 60.0) * cost_per_hour_snapshot
        END
    ) STORED,
    created_at                        TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                        TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT production_order_machine_usage_pkey PRIMARY KEY (production_order_machine_usage_id),
    CONSTRAINT production_order_machine_usage_order_phase_machine_unique
        UNIQUE (production_order_id, phase, machine_id),
    CONSTRAINT production_order_machine_usage_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_machine_usage_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_machine_usage_machine_fk
        FOREIGN KEY (machine_id) REFERENCES indicolors.machines (machine_id),
    CONSTRAINT production_order_machine_usage_phase_check
        CHECK (phase IN ('preprensa','corte-papel','impresion','terminados','acabados')),
    CONSTRAINT production_order_machine_usage_estimated_setup_check CHECK (estimated_setup_minutes >= 0),
    CONSTRAINT production_order_machine_usage_estimated_run_check CHECK (estimated_run_minutes >= 0),
    CONSTRAINT production_order_machine_usage_actual_setup_check CHECK (actual_setup_minutes IS NULL OR actual_setup_minutes >= 0),
    CONSTRAINT production_order_machine_usage_actual_run_check CHECK (actual_run_minutes IS NULL OR actual_run_minutes >= 0)
);

CREATE INDEX idx_production_order_machine_usage_company_id ON indicolors.production_order_machine_usage (company_id);
CREATE INDEX idx_production_order_machine_usage_order_id ON indicolors.production_order_machine_usage (production_order_id);
CREATE INDEX idx_production_order_machine_usage_machine_id ON indicolors.production_order_machine_usage (machine_id);
CREATE INDEX idx_production_order_machine_usage_phase ON indicolors.production_order_machine_usage (production_order_id, phase);

COMMENT ON TABLE indicolors.production_order_machine_usage IS 'Uso de máquina por fase de una Orden de Producción (estimado al cotizar, real al cerrar la fase). Esta es la tabla que faltaba para dejar de costear solo por cantidad solicitada + material, y sí cargar el costo fijo real de la máquina en cada operación';
COMMENT ON COLUMN indicolors.production_order_machine_usage.production_order_machine_usage_id IS 'Identificador único del registro de uso de máquina';
COMMENT ON COLUMN indicolors.production_order_machine_usage.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_machine_usage.production_order_id IS 'Identificador de la Orden de Producción a la que pertenece';
COMMENT ON COLUMN indicolors.production_order_machine_usage.phase IS 'Fase del wizard en la que se usó la máquina (mismo dominio que station_operation_events.phase)';
COMMENT ON COLUMN indicolors.production_order_machine_usage.machine_id IS 'Identificador de la máquina usada (FK a machines)';
COMMENT ON COLUMN indicolors.production_order_machine_usage.machine_name_snapshot IS 'Snapshot del nombre de la máquina al momento de guardar';
COMMENT ON COLUMN indicolors.production_order_machine_usage.cost_per_hour_snapshot IS 'Snapshot del costo/hora de la máquina al momento de guardar; no debe recalcularse si luego cambia la tarifa de la máquina, para no alterar el costo histórico de la OP';
COMMENT ON COLUMN indicolors.production_order_machine_usage.estimated_setup_minutes IS 'Minutos estimados de arranque/calibración (montaje, registro de color) al cotizar';
COMMENT ON COLUMN indicolors.production_order_machine_usage.estimated_run_minutes IS 'Minutos estimados de producción efectiva al cotizar';
COMMENT ON COLUMN indicolors.production_order_machine_usage.estimated_machine_cost IS 'Costo de máquina estimado, calculado automáticamente: (minutos de arranque + minutos de producción) / 60 * costo/hora snapshot';
COMMENT ON COLUMN indicolors.production_order_machine_usage.actual_setup_minutes IS 'Minutos reales de arranque, capturados al cerrar la fase en el módulo Estación; NULL hasta que se registre';
COMMENT ON COLUMN indicolors.production_order_machine_usage.actual_run_minutes IS 'Minutos reales de producción, capturados al cerrar la fase; NULL hasta que se registre';
COMMENT ON COLUMN indicolors.production_order_machine_usage.actual_machine_cost IS 'Costo de máquina real, calculado automáticamente igual que el estimado pero con minutos reales; NULL mientras no se haya capturado ningún dato real';
COMMENT ON COLUMN indicolors.production_order_machine_usage.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_machine_usage.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_machine_usage TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_machine_usage TO indicolors_app;

-- ============================================================================
-- 50.3 MERMAS Y DESPERDICIOS VALORIZADOS POR FASE
-- ============================================================================
CREATE TABLE indicolors.production_order_waste_records (
    production_order_waste_record_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id                       CHARACTER VARYING(64)       NOT NULL,
    production_order_id              CHARACTER VARYING(64)       NOT NULL,
    phase                            CHARACTER VARYING(20)       NOT NULL,
    waste_category                   CHARACTER VARYING(20)       NOT NULL,
    waste_origin                     CHARACTER VARYING(20)       NOT NULL DEFAULT 'exceso',
    material_type                    CHARACTER VARYING(20)       NOT NULL,
    paper_row_id                     CHARACTER VARYING(64),
    postpress_line_id                CHARACTER VARYING(64),
    planned_quantity                 NUMERIC(12,2)                NOT NULL DEFAULT 0,
    actual_quantity                  NUMERIC(12,2),
    unit_cost_snapshot               NUMERIC(12,2)                NOT NULL,
    planned_cost                     NUMERIC(12,2) GENERATED ALWAYS AS (planned_quantity * unit_cost_snapshot) STORED,
    actual_cost                      NUMERIC(12,2) GENERATED ALWAYS AS (
        CASE WHEN actual_quantity IS NULL THEN NULL ELSE actual_quantity * unit_cost_snapshot END
    ) STORED,
    note                             TEXT,
    created_at                       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                       TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT production_order_waste_records_pkey PRIMARY KEY (production_order_waste_record_id),
    CONSTRAINT production_order_waste_records_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_waste_records_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE,
    CONSTRAINT production_order_waste_records_paper_row_fk
        FOREIGN KEY (paper_row_id) REFERENCES indicolors.production_order_paper_rows (production_order_paper_row_id) ON DELETE SET NULL,
    CONSTRAINT production_order_waste_records_postpress_line_fk
        FOREIGN KEY (postpress_line_id) REFERENCES indicolors.production_order_postpress_lines (production_order_postpress_line_id) ON DELETE SET NULL,
    CONSTRAINT production_order_waste_records_phase_check
        CHECK (phase IN ('preprensa','corte-papel','impresion','terminados','acabados')),
    CONSTRAINT production_order_waste_records_category_check
        CHECK (waste_category IN ('merma_corte','merma_operativa','merma_administrativa','desperdicio')),
    CONSTRAINT production_order_waste_records_origin_check
        CHECK (waste_origin IN ('exceso','retrabajo')),
    CONSTRAINT production_order_waste_records_material_type_check
        CHECK (material_type IN ('papel','tinta','plancha','acabado','otro')),
    CONSTRAINT production_order_waste_records_planned_quantity_check CHECK (planned_quantity >= 0),
    CONSTRAINT production_order_waste_records_actual_quantity_check CHECK (actual_quantity IS NULL OR actual_quantity >= 0),
    CONSTRAINT production_order_waste_records_unit_cost_check CHECK (unit_cost_snapshot >= 0)
);

CREATE INDEX idx_production_order_waste_records_company_id ON indicolors.production_order_waste_records (company_id);
CREATE INDEX idx_production_order_waste_records_order_id ON indicolors.production_order_waste_records (production_order_id);
CREATE INDEX idx_production_order_waste_records_phase ON indicolors.production_order_waste_records (production_order_id, phase);
CREATE INDEX idx_production_order_waste_records_category ON indicolors.production_order_waste_records (waste_category);
CREATE INDEX idx_production_order_waste_records_paper_row_id ON indicolors.production_order_waste_records (paper_row_id);
CREATE INDEX idx_production_order_waste_records_postpress_line_id ON indicolors.production_order_waste_records (postpress_line_id);

COMMENT ON TABLE indicolors.production_order_waste_records IS 'Mermas (planificables: corte/operativa/administrativa) y desperdicios (no planificados) valorizados por fase de la Orden de Producción; base para comparar costo cotizado vs. costo real';
COMMENT ON COLUMN indicolors.production_order_waste_records.production_order_waste_record_id IS 'Identificador único del registro de merma/desperdicio';
COMMENT ON COLUMN indicolors.production_order_waste_records.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_waste_records.production_order_id IS 'Identificador de la Orden de Producción a la que pertenece';
COMMENT ON COLUMN indicolors.production_order_waste_records.phase IS 'Fase del wizard en la que ocurre la merma/desperdicio';
COMMENT ON COLUMN indicolors.production_order_waste_records.waste_category IS 'merma_corte y merma_operativa son planificables/predecibles (se cotizan de antemano); merma_administrativa es diferencia de inventario/almacenaje; desperdicio es lo NO planificado, la fuga real de rentabilidad';
COMMENT ON COLUMN indicolors.production_order_waste_records.waste_origin IS 'exceso = lo que supera la merma al cerrar la fase; retrabajo = reimpresión o reproceso pagado por el taller. La aplicación solo distingue el origen cuando waste_category = desperdicio; el resto queda en exceso';
COMMENT ON COLUMN indicolors.production_order_waste_records.material_type IS 'Tipo de insumo afectado por la merma/desperdicio';
COMMENT ON COLUMN indicolors.production_order_waste_records.paper_row_id IS 'Fila de corte de papel asociada, cuando material_type=papel (FK opcional a production_order_paper_rows)';
COMMENT ON COLUMN indicolors.production_order_waste_records.postpress_line_id IS 'Línea de Terminados/Acabados asociada, cuando material_type=acabado (FK opcional a production_order_postpress_lines)';
COMMENT ON COLUMN indicolors.production_order_waste_records.planned_quantity IS 'Cantidad planificada. En corte-papel e impresión: pliegos fijos de arranque + base * porcentaje / 100. En preprensa, terminados y acabados: solo porcentaje sobre planchas o piezas. En desperdicio queda en 0';
COMMENT ON COLUMN indicolors.production_order_waste_records.actual_quantity IS 'En la merma, la parte de la cantidad observada que cabe en planned_quantity. El exceso va en otro registro desperdicio. NULL hasta el cierre de fase';
COMMENT ON COLUMN indicolors.production_order_waste_records.unit_cost_snapshot IS 'Costo unitario del material en el momento del registro. En desperdicio incluye la prorrata de material de la merma, máquina de la fase y, solo en impresión, tinta';
COMMENT ON COLUMN indicolors.production_order_waste_records.planned_cost IS 'Costo de la merma planificada, calculado automáticamente: planned_quantity * unit_cost_snapshot';
COMMENT ON COLUMN indicolors.production_order_waste_records.actual_cost IS 'Costo de la merma o del desperdicio real, calculado automáticamente; NULL mientras no se capture actual_quantity';
COMMENT ON COLUMN indicolors.production_order_waste_records.note IS 'En desperdicio, etiqueta del motivo (papel de mala calidad, mal cortado, ajuste de registro/color, cambio a medio tiro, defecto de impresión, otro). En la merma planificada de corte e impresión guarda el marcador arranque:<pliegos> para devolver los pliegos fijos al reabrir la orden';
COMMENT ON COLUMN indicolors.production_order_waste_records.created_at IS 'Fecha y hora de creación del registro';
COMMENT ON COLUMN indicolors.production_order_waste_records.updated_at IS 'Fecha y hora de la última actualización del registro';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_waste_records TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_waste_records TO indicolors_app;

-- ============================================================================
-- 50.5 RESUMEN DE COSTOS POR ORDEN (tabla DERIVADA, mantenida por triggers,
-- mismo patrón que indicolors.accounts_receivable: nunca queda a cargo del
-- backend recordar sincronizarla)
-- ============================================================================
CREATE TABLE indicolors.production_order_cost_summary (
    production_order_id      CHARACTER VARYING(64)       NOT NULL,
    company_id                CHARACTER VARYING(64)       NOT NULL,
    estimated_material_cost   NUMERIC(14,2)               NOT NULL DEFAULT 0,
    estimated_machine_cost    NUMERIC(14,2)               NOT NULL DEFAULT 0,
    estimated_waste_cost      NUMERIC(14,2)               NOT NULL DEFAULT 0,
    estimated_merma_cost      NUMERIC(14,2)               NOT NULL DEFAULT 0,
    estimated_total_cost      NUMERIC(14,2)               NOT NULL DEFAULT 0,
    actual_material_cost      NUMERIC(14,2)               NOT NULL DEFAULT 0,
    actual_machine_cost       NUMERIC(14,2)               NOT NULL DEFAULT 0,
    actual_waste_cost         NUMERIC(14,2)               NOT NULL DEFAULT 0,
    actual_merma_cost         NUMERIC(14,2)               NOT NULL DEFAULT 0,
    actual_desperdicio_cost   NUMERIC(14,2)               NOT NULL DEFAULT 0,
    actual_total_cost         NUMERIC(14,2)               NOT NULL DEFAULT 0,
    quoted_price               NUMERIC(14,2),
    estimated_margin           NUMERIC(14,2) GENERATED ALWAYS AS (
        CASE WHEN quoted_price IS NULL THEN NULL ELSE quoted_price - estimated_total_cost END
    ) STORED,
    actual_margin               NUMERIC(14,2) GENERATED ALWAYS AS (
        CASE WHEN quoted_price IS NULL THEN NULL ELSE quoted_price - actual_total_cost END
    ) STORED,
    actual_margin_pct           NUMERIC(6,2) GENERATED ALWAYS AS (
        CASE WHEN quoted_price IS NULL OR quoted_price = 0 THEN NULL
             ELSE ROUND(((quoted_price - actual_total_cost) / quoted_price) * 100, 2)
        END
    ) STORED,
    updated_at                 TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT production_order_cost_summary_pkey PRIMARY KEY (production_order_id),
    CONSTRAINT production_order_cost_summary_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT production_order_cost_summary_order_fk
        FOREIGN KEY (production_order_id) REFERENCES indicolors.production_orders (production_order_id) ON DELETE CASCADE
);

CREATE INDEX idx_production_order_cost_summary_company_id ON indicolors.production_order_cost_summary (company_id);
CREATE INDEX idx_production_order_cost_summary_actual_margin_pct ON indicolors.production_order_cost_summary (company_id, actual_margin_pct);

COMMENT ON TABLE indicolors.production_order_cost_summary IS 'Resumen derivado de costos por Orden de Producción (1:1 con production_orders), recalculado automáticamente por triggers al escribir en paper_rows, prepress_details, postpress_lines, machine_usage o waste_records. Es la fuente para el reporte de Rentabilidad por Orden (costo cotizado vs. costo real, margen real)';
COMMENT ON COLUMN indicolors.production_order_cost_summary.production_order_id IS 'Identificador de la Orden de Producción (mismo id que production_orders, relación 1:1)';
COMMENT ON COLUMN indicolors.production_order_cost_summary.company_id IS 'Identificador de la empresa dueña del registro';
COMMENT ON COLUMN indicolors.production_order_cost_summary.estimated_material_cost IS 'Papel + corte + planchas + diseño + terminados/acabados, tomado de las tablas fuente (es el mismo valor que actual_material_cost hasta que el sistema capture consumo real de material distinto al cotizado)';
COMMENT ON COLUMN indicolors.production_order_cost_summary.estimated_machine_cost IS 'Suma de production_order_machine_usage.estimated_machine_cost de todas las fases de la OP';
COMMENT ON COLUMN indicolors.production_order_cost_summary.estimated_waste_cost IS 'Suma de planned_cost de todos los registros. El desperdicio aporta 0 porque su planned_quantity es 0';
COMMENT ON COLUMN indicolors.production_order_cost_summary.estimated_merma_cost IS 'Suma de planned_cost donde waste_category es distinto de desperdicio';
COMMENT ON COLUMN indicolors.production_order_cost_summary.estimated_total_cost IS 'estimated_material_cost + estimated_machine_cost + estimated_waste_cost';
COMMENT ON COLUMN indicolors.production_order_cost_summary.actual_material_cost IS 'Igual a estimated_material_cost (ver comentario de esa columna); se deja separado para permitir a futuro capturar consumo real de material distinto al cotizado sin cambiar el esquema';
COMMENT ON COLUMN indicolors.production_order_cost_summary.actual_machine_cost IS 'Suma de production_order_machine_usage.actual_machine_cost cuando existe; si una fase aún no tiene dato real, se usa su estimated_machine_cost como mejor aproximación disponible';
COMMENT ON COLUMN indicolors.production_order_cost_summary.actual_waste_cost IS 'Suma de COALESCE(actual_cost, planned_cost) de todos los registros. Incluye merma real y desperdicio';
COMMENT ON COLUMN indicolors.production_order_cost_summary.actual_merma_cost IS 'Suma de COALESCE(actual_cost, planned_cost) donde waste_category es distinto de desperdicio';
COMMENT ON COLUMN indicolors.production_order_cost_summary.actual_desperdicio_cost IS 'Suma de COALESCE(actual_cost, 0) donde waste_category = desperdicio. No entra en el precio cobrado; sí entra en actual_waste_cost y por tanto en actual_total_cost';
COMMENT ON COLUMN indicolors.production_order_cost_summary.actual_total_cost IS 'actual_material_cost + actual_machine_cost + actual_waste_cost';
COMMENT ON COLUMN indicolors.production_order_cost_summary.quoted_price IS 'Precio final cotizado al cliente (totalToCharge). Lo escribe el backend al cotizar, no este trigger, y no cambia al registrar desperdicio o retrabajo';
COMMENT ON COLUMN indicolors.production_order_cost_summary.estimated_margin IS 'quoted_price - estimated_total_cost; NULL mientras no se conozca quoted_price';
COMMENT ON COLUMN indicolors.production_order_cost_summary.actual_margin IS 'quoted_price - actual_total_cost; NULL mientras no se conozca quoted_price. Esta es la ganancia neta real de la OP, no el margen que se puso en la cotización';
COMMENT ON COLUMN indicolors.production_order_cost_summary.actual_margin_pct IS 'Margen real como % del precio cotizado; usado para resaltar en el reporte las órdenes que están dejando pérdida';
COMMENT ON COLUMN indicolors.production_order_cost_summary.updated_at IS 'Fecha y hora del último recálculo automático';

GRANT ALL PRIVILEGES ON TABLE indicolors.production_order_cost_summary TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.production_order_cost_summary TO indicolors_app;

-- ============================================================================
-- 50.6 FUNCIÓN COMPARTIDA DE RECÁLCULO + TRIGGERS EN TODAS LAS TABLAS FUENTE
-- ============================================================================
CREATE OR REPLACE FUNCTION indicolors.fn_recalc_production_order_cost_summary(
    p_production_order_id CHARACTER VARYING(64),
    p_company_id           CHARACTER VARYING(64)
)
RETURNS VOID AS $$
DECLARE
    v_material_cost        NUMERIC(14,2);
    v_machine_est          NUMERIC(14,2);
    v_machine_act          NUMERIC(14,2);
    v_waste_est            NUMERIC(14,2);
    v_waste_act            NUMERIC(14,2);
    v_merma_est            NUMERIC(14,2);
    v_merma_act            NUMERIC(14,2);
    v_desperdicio_act      NUMERIC(14,2);
    v_quoted_price         NUMERIC(14,2);
BEGIN
    -- Material: papel + corte (paper_rows) + planchas/diseño (prepress_details)
    -- + terminados/acabados (postpress_lines vía postpress_records)
    SELECT COALESCE(SUM(pr.total_paper_value), 0) + COALESCE(SUM(pr.total_cut_value), 0)
    INTO v_material_cost
    FROM indicolors.production_order_paper_rows pr
    WHERE pr.production_order_id = p_production_order_id;

    v_material_cost := v_material_cost
        + COALESCE((
            SELECT COALESCE(pd.total_plates_value, 0)
                 + COALESCE(pd.design_cost, 0)
                 + COALESCE(pd.new_plate_cost, 0)
            FROM indicolors.production_order_prepress_details pd
            WHERE pd.production_order_id = p_production_order_id
          ), 0)
        + COALESCE((
            SELECT SUM(pl.charged_price)
            FROM indicolors.production_order_postpress_lines pl
            JOIN indicolors.production_order_postpress_records rec
                ON rec.production_order_postpress_record_id = pl.record_id
            WHERE rec.production_order_id = p_production_order_id
          ), 0);

    -- Máquina: estimado vs real (real cae al estimado mientras no se capture)
    SELECT COALESCE(SUM(mu.estimated_machine_cost), 0),
           COALESCE(SUM(COALESCE(mu.actual_machine_cost, mu.estimated_machine_cost)), 0)
    INTO v_machine_est, v_machine_act
    FROM indicolors.production_order_machine_usage mu
    WHERE mu.production_order_id = p_production_order_id;

    -- Merma + desperdicio juntos. El desperdicio aporta 0 al estimado
    -- porque su planned_quantity es 0. El real incluye merma y desperdicio.
    SELECT COALESCE(SUM(wr.planned_cost), 0),
           COALESCE(SUM(COALESCE(wr.actual_cost, wr.planned_cost)), 0)
    INTO v_waste_est, v_waste_act
    FROM indicolors.production_order_waste_records wr
    WHERE wr.production_order_id = p_production_order_id;

    SELECT COALESCE(SUM(wr.planned_cost), 0),
           COALESCE(SUM(COALESCE(wr.actual_cost, wr.planned_cost)), 0)
    INTO v_merma_est, v_merma_act
    FROM indicolors.production_order_waste_records wr
    WHERE wr.production_order_id = p_production_order_id
      AND wr.waste_category <> 'desperdicio';

    SELECT COALESCE(SUM(COALESCE(wr.actual_cost, 0)), 0)
    INTO v_desperdicio_act
    FROM indicolors.production_order_waste_records wr
    WHERE wr.production_order_id = p_production_order_id
      AND wr.waste_category = 'desperdicio';

    -- Conserva el quoted_price que ya estuviera guardado (lo fija el backend,
    -- no este trigger)
    SELECT cs.quoted_price INTO v_quoted_price
    FROM indicolors.production_order_cost_summary cs
    WHERE cs.production_order_id = p_production_order_id;

    INSERT INTO indicolors.production_order_cost_summary (
        production_order_id, company_id,
        estimated_material_cost, estimated_machine_cost, estimated_waste_cost, estimated_merma_cost, estimated_total_cost,
        actual_material_cost, actual_machine_cost, actual_waste_cost, actual_merma_cost, actual_desperdicio_cost, actual_total_cost,
        quoted_price, updated_at
    )
    VALUES (
        p_production_order_id, p_company_id,
        v_material_cost, v_machine_est, v_waste_est, v_merma_est, v_material_cost + v_machine_est + v_waste_est,
        v_material_cost, v_machine_act, v_waste_act, v_merma_act, v_desperdicio_act, v_material_cost + v_machine_act + v_waste_act,
        v_quoted_price, now()
    )
    ON CONFLICT (production_order_id) DO UPDATE SET
        estimated_material_cost = EXCLUDED.estimated_material_cost,
        estimated_machine_cost  = EXCLUDED.estimated_machine_cost,
        estimated_waste_cost    = EXCLUDED.estimated_waste_cost,
        estimated_merma_cost    = EXCLUDED.estimated_merma_cost,
        estimated_total_cost    = EXCLUDED.estimated_total_cost,
        actual_material_cost    = EXCLUDED.actual_material_cost,
        actual_machine_cost     = EXCLUDED.actual_machine_cost,
        actual_waste_cost       = EXCLUDED.actual_waste_cost,
        actual_merma_cost       = EXCLUDED.actual_merma_cost,
        actual_desperdicio_cost = EXCLUDED.actual_desperdicio_cost,
        actual_total_cost       = EXCLUDED.actual_total_cost,
        updated_at              = now();
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION indicolors.fn_recalc_production_order_cost_summary(CHARACTER VARYING, CHARACTER VARYING) IS
    'Recalcula y hace upsert de production_order_cost_summary para una OP a partir de paper_rows, prepress_details, postpress_lines, machine_usage y waste_records. Separa merma y desperdicio sin cambiar los totales que alimentan el margen. Se invoca desde triggers en cada tabla fuente, siempre dentro de la misma transacción de la escritura original';

-- --- Trigger wrappers por tabla fuente (cada uno resuelve production_order_id
-- --- y company_id según su propia estructura, y llama a la función compartida) ---

CREATE OR REPLACE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_paper_rows()
RETURNS TRIGGER AS $$
DECLARE
    v_order_id CHARACTER VARYING(64);
    v_company_id CHARACTER VARYING(64);
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_order_id := OLD.production_order_id;
        v_company_id := OLD.company_id;
    ELSE
        v_order_id := NEW.production_order_id;
        v_company_id := NEW.company_id;
    END IF;
    PERFORM indicolors.fn_recalc_production_order_cost_summary(v_order_id, v_company_id);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_paper_rows_recalc_cost_summary ON indicolors.production_order_paper_rows;
CREATE TRIGGER trg_paper_rows_recalc_cost_summary
    AFTER INSERT OR UPDATE OR DELETE ON indicolors.production_order_paper_rows
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_paper_rows();

CREATE OR REPLACE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_prepress_details()
RETURNS TRIGGER AS $$
DECLARE
    v_order_id CHARACTER VARYING(64);
    v_company_id CHARACTER VARYING(64);
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_order_id := OLD.production_order_id;
        v_company_id := OLD.company_id;
    ELSE
        v_order_id := NEW.production_order_id;
        v_company_id := NEW.company_id;
    END IF;
    PERFORM indicolors.fn_recalc_production_order_cost_summary(v_order_id, v_company_id);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prepress_details_recalc_cost_summary ON indicolors.production_order_prepress_details;
CREATE TRIGGER trg_prepress_details_recalc_cost_summary
    AFTER INSERT OR UPDATE OR DELETE ON indicolors.production_order_prepress_details
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_prepress_details();

CREATE OR REPLACE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_postpress_lines()
RETURNS TRIGGER AS $$
DECLARE
    v_order_id CHARACTER VARYING(64);
    v_company_id CHARACTER VARYING(64);
    v_record_id CHARACTER VARYING(64);
BEGIN
    v_record_id := CASE WHEN TG_OP = 'DELETE' THEN OLD.record_id ELSE NEW.record_id END;
    v_company_id := CASE WHEN TG_OP = 'DELETE' THEN OLD.company_id ELSE NEW.company_id END;

    SELECT rec.production_order_id INTO v_order_id
    FROM indicolors.production_order_postpress_records rec
    WHERE rec.production_order_postpress_record_id = v_record_id;

    IF v_order_id IS NOT NULL THEN
        PERFORM indicolors.fn_recalc_production_order_cost_summary(v_order_id, v_company_id);
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_postpress_lines_recalc_cost_summary ON indicolors.production_order_postpress_lines;
CREATE TRIGGER trg_postpress_lines_recalc_cost_summary
    AFTER INSERT OR UPDATE OR DELETE ON indicolors.production_order_postpress_lines
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_postpress_lines();

CREATE OR REPLACE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_machine_usage()
RETURNS TRIGGER AS $$
DECLARE
    v_order_id CHARACTER VARYING(64);
    v_company_id CHARACTER VARYING(64);
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_order_id := OLD.production_order_id;
        v_company_id := OLD.company_id;
    ELSE
        v_order_id := NEW.production_order_id;
        v_company_id := NEW.company_id;
    END IF;
    PERFORM indicolors.fn_recalc_production_order_cost_summary(v_order_id, v_company_id);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_machine_usage_recalc_cost_summary ON indicolors.production_order_machine_usage;
CREATE TRIGGER trg_machine_usage_recalc_cost_summary
    AFTER INSERT OR UPDATE OR DELETE ON indicolors.production_order_machine_usage
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_machine_usage();

CREATE OR REPLACE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_waste_records()
RETURNS TRIGGER AS $$
DECLARE
    v_order_id CHARACTER VARYING(64);
    v_company_id CHARACTER VARYING(64);
BEGIN
    IF TG_OP = 'DELETE' THEN
        v_order_id := OLD.production_order_id;
        v_company_id := OLD.company_id;
    ELSE
        v_order_id := NEW.production_order_id;
        v_company_id := NEW.company_id;
    END IF;
    PERFORM indicolors.fn_recalc_production_order_cost_summary(v_order_id, v_company_id);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_waste_records_recalc_cost_summary ON indicolors.production_order_waste_records;
CREATE TRIGGER trg_waste_records_recalc_cost_summary
    AFTER INSERT OR UPDATE OR DELETE ON indicolors.production_order_waste_records
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_trg_recalc_cost_summary_from_waste_records();

COMMENT ON FUNCTION indicolors.fn_trg_recalc_cost_summary_from_paper_rows() IS 'Dispara el recálculo de production_order_cost_summary tras cualquier cambio en production_order_paper_rows';
COMMENT ON FUNCTION indicolors.fn_trg_recalc_cost_summary_from_prepress_details() IS 'Dispara el recálculo de production_order_cost_summary tras cualquier cambio en production_order_prepress_details';
COMMENT ON FUNCTION indicolors.fn_trg_recalc_cost_summary_from_postpress_lines() IS 'Dispara el recálculo de production_order_cost_summary tras cualquier cambio en production_order_postpress_lines, resolviendo production_order_id vía production_order_postpress_records';
COMMENT ON FUNCTION indicolors.fn_trg_recalc_cost_summary_from_machine_usage() IS 'Dispara el recálculo de production_order_cost_summary tras cualquier cambio en production_order_machine_usage';
COMMENT ON FUNCTION indicolors.fn_trg_recalc_cost_summary_from_waste_records() IS 'Dispara el recálculo de production_order_cost_summary tras cualquier cambio en production_order_waste_records';

-- ============================================================================
-- 50.7 REFUERZO DE PERMISOS (por si alguna tabla queda sin GRANT explícito)
-- ============================================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA indicolors TO indicolors_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA indicolors TO indicolors_app;

-- ============================================================================
-- 50.8 RANGOS DE MERMA Y PLIEGOS FIJOS DE ARRANQUE POR COMPAÑÍA
-- El backend no hardcodea 2%-5% / 3%-8%: son el valor inicial. La UI los
-- muestra como sugerencia; el default se aplica si el usuario no lo cambia.
-- Los pliegos fijos nacen en 0. En corte e impresión la cantidad planificada
-- es pliegos fijos + base * porcentaje / 100. El ejemplo 400 + 2% no se siembra.
-- ============================================================================
CREATE TABLE indicolors.company_waste_settings (
    company_id                              CHARACTER VARYING(64) NOT NULL,
    cut_waste_min_percentage                NUMERIC(5,2)          NOT NULL DEFAULT 2,
    cut_waste_max_percentage                NUMERIC(5,2)          NOT NULL DEFAULT 5,
    cut_waste_default_percentage            NUMERIC(5,2)          NOT NULL DEFAULT 2,
    operational_waste_min_percentage        NUMERIC(5,2)          NOT NULL DEFAULT 3,
    operational_waste_max_percentage        NUMERIC(5,2)          NOT NULL DEFAULT 8,
    operational_waste_default_percentage    NUMERIC(5,2)          NOT NULL DEFAULT 3,
    prepress_waste_min_percentage           NUMERIC(5,2)          NOT NULL DEFAULT 3,
    prepress_waste_max_percentage           NUMERIC(5,2)          NOT NULL DEFAULT 8,
    prepress_waste_default_percentage       NUMERIC(5,2)          NOT NULL DEFAULT 3,
    finished_waste_min_percentage           NUMERIC(5,2)          NOT NULL DEFAULT 3,
    finished_waste_max_percentage           NUMERIC(5,2)          NOT NULL DEFAULT 8,
    finished_waste_default_percentage       NUMERIC(5,2)          NOT NULL DEFAULT 3,
    finishing_waste_min_percentage          NUMERIC(5,2)          NOT NULL DEFAULT 3,
    finishing_waste_max_percentage          NUMERIC(5,2)          NOT NULL DEFAULT 8,
    finishing_waste_default_percentage      NUMERIC(5,2)          NOT NULL DEFAULT 3,
    cut_makeready_sheets                    NUMERIC(12,2)         NOT NULL DEFAULT 0,
    operational_makeready_sheets            NUMERIC(12,2)         NOT NULL DEFAULT 0,
    updated_at                              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT company_waste_settings_pkey PRIMARY KEY (company_id),
    CONSTRAINT company_waste_settings_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT company_waste_settings_cut_range_check
        CHECK (cut_waste_min_percentage >= 0 AND cut_waste_min_percentage <= cut_waste_max_percentage
               AND cut_waste_max_percentage <= 100
               AND cut_waste_default_percentage >= cut_waste_min_percentage
               AND cut_waste_default_percentage <= cut_waste_max_percentage),
    CONSTRAINT company_waste_settings_operational_range_check
        CHECK (operational_waste_min_percentage >= 0
               AND operational_waste_min_percentage <= operational_waste_max_percentage
               AND operational_waste_max_percentage <= 100
               AND operational_waste_default_percentage >= operational_waste_min_percentage
               AND operational_waste_default_percentage <= operational_waste_max_percentage),
    CONSTRAINT company_waste_settings_cut_makeready_check
        CHECK (cut_makeready_sheets >= 0),
    CONSTRAINT company_waste_settings_operational_makeready_check
        CHECK (operational_makeready_sheets >= 0),
    CONSTRAINT company_waste_settings_prepress_range_check
        CHECK (prepress_waste_min_percentage >= 0
               AND prepress_waste_min_percentage <= prepress_waste_max_percentage
               AND prepress_waste_max_percentage <= 100
               AND prepress_waste_default_percentage >= prepress_waste_min_percentage
               AND prepress_waste_default_percentage <= prepress_waste_max_percentage),
    CONSTRAINT company_waste_settings_finished_range_check
        CHECK (finished_waste_min_percentage >= 0
               AND finished_waste_min_percentage <= finished_waste_max_percentage
               AND finished_waste_max_percentage <= 100
               AND finished_waste_default_percentage >= finished_waste_min_percentage
               AND finished_waste_default_percentage <= finished_waste_max_percentage),
    CONSTRAINT company_waste_settings_finishing_range_check
        CHECK (finishing_waste_min_percentage >= 0
               AND finishing_waste_min_percentage <= finishing_waste_max_percentage
               AND finishing_waste_max_percentage <= 100
               AND finishing_waste_default_percentage >= finishing_waste_min_percentage
               AND finishing_waste_default_percentage <= finishing_waste_max_percentage)
);

COMMENT ON TABLE indicolors.company_waste_settings IS 'Rangos sugeridos, porcentaje por defecto y pliegos fijos de arranque de merma, configurables por compañía y por proceso';
COMMENT ON COLUMN indicolors.company_waste_settings.cut_waste_min_percentage IS 'Límite inferior sugerido en UI para merma_corte (inicial 2%)';
COMMENT ON COLUMN indicolors.company_waste_settings.cut_waste_max_percentage IS 'Límite superior sugerido en UI para merma_corte (inicial 5%)';
COMMENT ON COLUMN indicolors.company_waste_settings.cut_waste_default_percentage IS 'Porcentaje aplicado en Corte de papel si el usuario no lo cambia';
COMMENT ON COLUMN indicolors.company_waste_settings.operational_waste_min_percentage IS 'Límite inferior sugerido en UI para merma_operativa de impresión (inicial 3%)';
COMMENT ON COLUMN indicolors.company_waste_settings.operational_waste_max_percentage IS 'Límite superior sugerido en UI para merma_operativa de impresión (inicial 8%)';
COMMENT ON COLUMN indicolors.company_waste_settings.operational_waste_default_percentage IS 'Porcentaje aplicado en Impresión si el usuario no lo cambia';
COMMENT ON COLUMN indicolors.company_waste_settings.prepress_waste_min_percentage IS 'Límite inferior sugerido en UI para la merma de Preprensa. Inicialmente copia la merma operativa para no cambiar las órdenes ya cotizadas';
COMMENT ON COLUMN indicolors.company_waste_settings.prepress_waste_max_percentage IS 'Límite superior sugerido en UI para la merma de Preprensa';
COMMENT ON COLUMN indicolors.company_waste_settings.prepress_waste_default_percentage IS 'Porcentaje aplicado en Preprensa si el usuario no lo cambia';
COMMENT ON COLUMN indicolors.company_waste_settings.finished_waste_min_percentage IS 'Límite inferior sugerido en UI para la merma de Terminados. Inicialmente copia la merma operativa';
COMMENT ON COLUMN indicolors.company_waste_settings.finished_waste_max_percentage IS 'Límite superior sugerido en UI para la merma de Terminados';
COMMENT ON COLUMN indicolors.company_waste_settings.finished_waste_default_percentage IS 'Porcentaje aplicado en Terminados si el usuario no lo cambia';
COMMENT ON COLUMN indicolors.company_waste_settings.finishing_waste_min_percentage IS 'Límite inferior sugerido en UI para la merma de Acabados. Inicialmente copia la merma operativa';
COMMENT ON COLUMN indicolors.company_waste_settings.finishing_waste_max_percentage IS 'Límite superior sugerido en UI para la merma de Acabados';
COMMENT ON COLUMN indicolors.company_waste_settings.finishing_waste_default_percentage IS 'Porcentaje aplicado en Acabados si el usuario no lo cambia';
COMMENT ON COLUMN indicolors.company_waste_settings.cut_makeready_sheets IS 'Pliegos fijos de arranque de Corte de papel. Default 0 para no cambiar la cantidad planificada de las órdenes actuales; el ejemplo de UI 400 + 2% no se siembra aquí';
COMMENT ON COLUMN indicolors.company_waste_settings.operational_makeready_sheets IS 'Pliegos fijos de arranque de Impresión. Default 0 para no cambiar la cantidad planificada de las órdenes actuales; el ejemplo de UI 400 + 2% no se siembra aquí';

GRANT ALL PRIVILEGES ON TABLE indicolors.company_waste_settings TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.company_waste_settings TO indicolors_app;

-- ============================================================================
-- FIN DE LA MIGRACIÓN
-- ============================================================================
