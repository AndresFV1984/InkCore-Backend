-- Tabla finished_products (formulario "Nuevo terminado") — un archivo por tabla, DDL completo.

CREATE TABLE IF NOT EXISTS indicolors.finished_products (
    finished_product_id VARCHAR(64)    NOT NULL DEFAULT gen_random_uuid()::text,
    company_id          VARCHAR(64)    NOT NULL,
    name                VARCHAR(150)   NOT NULL,
    min_cost            NUMERIC(12,2),
    value_per_cm2       NUMERIC(6,2)   NOT NULL DEFAULT 0,
    quick_access        BOOLEAN        NOT NULL DEFAULT FALSE,
    state               BOOLEAN        NOT NULL DEFAULT TRUE,
    creation_date       DATE           NOT NULL DEFAULT CURRENT_DATE,
    CONSTRAINT finished_products_pkey PRIMARY KEY (finished_product_id),
    CONSTRAINT finished_products_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT finished_products_name_company_unique
        UNIQUE (company_id, name),
    CONSTRAINT finished_products_min_cost_check
        CHECK (min_cost IS NULL OR min_cost >= 0),
    CONSTRAINT finished_products_value_per_cm2_check
        CHECK (value_per_cm2 >= 0 AND value_per_cm2 <= 9999)
);

CREATE INDEX IF NOT EXISTS idx_finished_products_company_id ON indicolors.finished_products (company_id);
CREATE INDEX IF NOT EXISTS idx_finished_products_name ON indicolors.finished_products (name);
CREATE INDEX IF NOT EXISTS idx_finished_products_state ON indicolors.finished_products (state);
CREATE INDEX IF NOT EXISTS idx_finished_products_company_state ON indicolors.finished_products (company_id, state);
CREATE INDEX IF NOT EXISTS idx_finished_products_quick_access ON indicolors.finished_products (quick_access);

COMMENT ON TABLE indicolors.finished_products IS 'Catálogo de Productos Terminados (ej. Laminado mate), usados al configurar órdenes de producción';
COMMENT ON COLUMN indicolors.finished_products.finished_product_id IS 'Identificador único del producto terminado';
COMMENT ON COLUMN indicolors.finished_products.company_id IS 'Identificador de la empresa dueña del producto terminado';
COMMENT ON COLUMN indicolors.finished_products.name IS 'Nombre del producto terminado (ej. Laminado mate)';
COMMENT ON COLUMN indicolors.finished_products.min_cost IS 'Costo mínimo del producto terminado; NULL si no aplica';
COMMENT ON COLUMN indicolors.finished_products.value_per_cm2 IS 'Valor por cm² del producto terminado; 0 si no aplica';
COMMENT ON COLUMN indicolors.finished_products.quick_access IS 'True=Muestra el producto terminado en la barra de selección rápida al configurar una orden de producción';
COMMENT ON COLUMN indicolors.finished_products.state IS 'True=Activo, False=Inactivo';
COMMENT ON COLUMN indicolors.finished_products.creation_date IS 'Fecha de registro del producto terminado en el sistema';
