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
