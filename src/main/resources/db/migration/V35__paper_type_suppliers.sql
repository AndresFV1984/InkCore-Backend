-- Relación N:M entre tipos de papel y proveedores, con valor hoja y unidad empaque por proveedor.

CREATE TABLE IF NOT EXISTS indicolors.paper_type_suppliers (
    paper_type_id   VARCHAR(64)     NOT NULL,
    supplier_id     VARCHAR(64)     NOT NULL,
    sheet_value     NUMERIC(12,2)   NOT NULL,
    package_unit    INTEGER         NOT NULL,
    assigned_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT paper_type_suppliers_pkey PRIMARY KEY (paper_type_id, supplier_id),
    CONSTRAINT paper_type_suppliers_paper_type_fk
        FOREIGN KEY (paper_type_id) REFERENCES indicolors.paper_types (paper_type_id) ON DELETE CASCADE,
    CONSTRAINT paper_type_suppliers_supplier_fk
        FOREIGN KEY (supplier_id) REFERENCES indicolors.suppliers (supplier_id) ON DELETE CASCADE,
    CONSTRAINT paper_type_suppliers_sheet_value_check CHECK (sheet_value >= 0),
    CONSTRAINT paper_type_suppliers_package_unit_check CHECK (package_unit > 0)
);

CREATE INDEX IF NOT EXISTS idx_paper_type_suppliers_supplier_id
    ON indicolors.paper_type_suppliers (supplier_id);

COMMENT ON TABLE indicolors.paper_type_suppliers IS 'Relación N:M entre tipos de papel y proveedores, con valor hoja y unidad empaque por proveedor';
COMMENT ON COLUMN indicolors.paper_type_suppliers.paper_type_id IS 'Identificador del tipo de papel';
COMMENT ON COLUMN indicolors.paper_type_suppliers.supplier_id IS 'Identificador del proveedor asociado';
COMMENT ON COLUMN indicolors.paper_type_suppliers.sheet_value IS 'Valor de la hoja/pliego para este proveedor';
COMMENT ON COLUMN indicolors.paper_type_suppliers.package_unit IS 'Cantidad de hojas por unidad de empaque para este proveedor';
