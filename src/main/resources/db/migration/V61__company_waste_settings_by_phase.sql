ALTER TABLE indicolors.company_waste_settings
    ADD COLUMN prepress_waste_min_percentage     NUMERIC(5,2) NOT NULL DEFAULT 3,
    ADD COLUMN prepress_waste_max_percentage     NUMERIC(5,2) NOT NULL DEFAULT 8,
    ADD COLUMN prepress_waste_default_percentage NUMERIC(5,2) NOT NULL DEFAULT 3,
    ADD COLUMN finished_waste_min_percentage     NUMERIC(5,2) NOT NULL DEFAULT 3,
    ADD COLUMN finished_waste_max_percentage     NUMERIC(5,2) NOT NULL DEFAULT 8,
    ADD COLUMN finished_waste_default_percentage NUMERIC(5,2) NOT NULL DEFAULT 3,
    ADD COLUMN finishing_waste_min_percentage     NUMERIC(5,2) NOT NULL DEFAULT 3,
    ADD COLUMN finishing_waste_max_percentage     NUMERIC(5,2) NOT NULL DEFAULT 8,
    ADD COLUMN finishing_waste_default_percentage NUMERIC(5,2) NOT NULL DEFAULT 3;

UPDATE indicolors.company_waste_settings
SET prepress_waste_min_percentage = operational_waste_min_percentage,
    prepress_waste_max_percentage = operational_waste_max_percentage,
    prepress_waste_default_percentage = operational_waste_default_percentage,
    finished_waste_min_percentage = operational_waste_min_percentage,
    finished_waste_max_percentage = operational_waste_max_percentage,
    finished_waste_default_percentage = operational_waste_default_percentage,
    finishing_waste_min_percentage = operational_waste_min_percentage,
    finishing_waste_max_percentage = operational_waste_max_percentage,
    finishing_waste_default_percentage = operational_waste_default_percentage;

ALTER TABLE indicolors.company_waste_settings
    ADD CONSTRAINT company_waste_settings_prepress_range_check
        CHECK (prepress_waste_min_percentage >= 0
               AND prepress_waste_min_percentage <= prepress_waste_max_percentage
               AND prepress_waste_max_percentage <= 100
               AND prepress_waste_default_percentage >= prepress_waste_min_percentage
               AND prepress_waste_default_percentage <= prepress_waste_max_percentage),
    ADD CONSTRAINT company_waste_settings_finished_range_check
        CHECK (finished_waste_min_percentage >= 0
               AND finished_waste_min_percentage <= finished_waste_max_percentage
               AND finished_waste_max_percentage <= 100
               AND finished_waste_default_percentage >= finished_waste_min_percentage
               AND finished_waste_default_percentage <= finished_waste_max_percentage),
    ADD CONSTRAINT company_waste_settings_finishing_range_check
        CHECK (finishing_waste_min_percentage >= 0
               AND finishing_waste_min_percentage <= finishing_waste_max_percentage
               AND finishing_waste_max_percentage <= 100
               AND finishing_waste_default_percentage >= finishing_waste_min_percentage
               AND finishing_waste_default_percentage <= finishing_waste_max_percentage);

COMMENT ON COLUMN indicolors.company_waste_settings.prepress_waste_min_percentage IS 'Límite inferior sugerido en UI para la merma de Preprensa. Inicialmente copia la merma operativa para no cambiar las órdenes ya cotizadas';
COMMENT ON COLUMN indicolors.company_waste_settings.prepress_waste_max_percentage IS 'Límite superior sugerido en UI para la merma de Preprensa';
COMMENT ON COLUMN indicolors.company_waste_settings.prepress_waste_default_percentage IS 'Porcentaje aplicado en Preprensa si el usuario no lo cambia';
COMMENT ON COLUMN indicolors.company_waste_settings.finished_waste_min_percentage IS 'Límite inferior sugerido en UI para la merma de Terminados. Inicialmente copia la merma operativa';
COMMENT ON COLUMN indicolors.company_waste_settings.finished_waste_max_percentage IS 'Límite superior sugerido en UI para la merma de Terminados';
COMMENT ON COLUMN indicolors.company_waste_settings.finished_waste_default_percentage IS 'Porcentaje aplicado en Terminados si el usuario no lo cambia';
COMMENT ON COLUMN indicolors.company_waste_settings.finishing_waste_min_percentage IS 'Límite inferior sugerido en UI para la merma de Acabados. Inicialmente copia la merma operativa';
COMMENT ON COLUMN indicolors.company_waste_settings.finishing_waste_max_percentage IS 'Límite superior sugerido en UI para la merma de Acabados';
COMMENT ON COLUMN indicolors.company_waste_settings.finishing_waste_default_percentage IS 'Porcentaje aplicado en Acabados si el usuario no lo cambia';
