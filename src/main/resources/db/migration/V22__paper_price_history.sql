CREATE TABLE indicolors.paper_price_history (
    paper_price_history_id CHARACTER VARYING(64)       NOT NULL DEFAULT gen_random_uuid()::text,
    company_id             CHARACTER VARYING(64)       NOT NULL,
    paper_id               CHARACTER VARYING(64)       NOT NULL,
    supplier_id            CHARACTER VARYING(64)       NOT NULL,
    sheet_value            NUMERIC(12,2)               NOT NULL,
    package_unit           INTEGER                     NOT NULL,
    freight_per_sheet      NUMERIC(12,2)               NOT NULL,
    min_purchase_sheets    INTEGER,
    payment_days           INTEGER,
    delivery_days          INTEGER,
    price_date             DATE                        NOT NULL,
    preferred              BOOLEAN                     NOT NULL,
    state                  BOOLEAN                     NOT NULL,
    effective_from         TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    changed_by             CHARACTER VARYING(64),
    CONSTRAINT paper_price_history_pkey PRIMARY KEY (paper_price_history_id),
    CONSTRAINT paper_price_history_company_fk
        FOREIGN KEY (company_id) REFERENCES indicolors.companies (company_id),
    CONSTRAINT paper_price_history_paper_fk
        FOREIGN KEY (paper_id) REFERENCES indicolors.papers (paper_id) ON DELETE CASCADE,
    CONSTRAINT paper_price_history_supplier_fk
        FOREIGN KEY (supplier_id) REFERENCES indicolors.suppliers (supplier_id),
    CONSTRAINT paper_price_history_changed_by_fk
        FOREIGN KEY (changed_by) REFERENCES indicolors.users (user_id)
);

CREATE INDEX idx_paper_price_history_company_id ON indicolors.paper_price_history (company_id);
CREATE INDEX idx_paper_price_history_paper_supplier_from
    ON indicolors.paper_price_history (paper_id, supplier_id, effective_from DESC);

COMMENT ON TABLE indicolors.paper_price_history IS 'Auditoría append-only de precios de papel por proveedor';
COMMENT ON COLUMN indicolors.paper_price_history.paper_price_history_id IS 'Identificador único del snapshot histórico';
COMMENT ON COLUMN indicolors.paper_price_history.paper_id IS 'Papel al que pertenecía el precio';
COMMENT ON COLUMN indicolors.paper_price_history.supplier_id IS 'Proveedor del snapshot';
COMMENT ON COLUMN indicolors.paper_price_history.package_unit IS 'Unidad de empaque (pliegos) del snapshot';
COMMENT ON COLUMN indicolors.paper_price_history.effective_from IS 'Fecha y hora desde la que aplica este snapshot';
COMMENT ON COLUMN indicolors.paper_price_history.changed_by IS 'Usuario que generó el cambio; NULL si fue proceso automático';

GRANT ALL PRIVILEGES ON TABLE indicolors.paper_price_history TO indicolors_owner;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE indicolors.paper_price_history TO indicolors_app;

CREATE OR REPLACE FUNCTION indicolors.fn_log_paper_price_history()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'UPDATE'
       AND NEW.sheet_value IS NOT DISTINCT FROM OLD.sheet_value
       AND NEW.package_unit IS NOT DISTINCT FROM OLD.package_unit
       AND NEW.freight_per_sheet IS NOT DISTINCT FROM OLD.freight_per_sheet
       AND NEW.min_purchase_sheets IS NOT DISTINCT FROM OLD.min_purchase_sheets
       AND NEW.payment_days IS NOT DISTINCT FROM OLD.payment_days
       AND NEW.delivery_days IS NOT DISTINCT FROM OLD.delivery_days
       AND NEW.price_date IS NOT DISTINCT FROM OLD.price_date
       AND NEW.preferred IS NOT DISTINCT FROM OLD.preferred
       AND NEW.state IS NOT DISTINCT FROM OLD.state THEN
        RETURN NEW;
    END IF;

    INSERT INTO indicolors.paper_price_history (
        company_id, paper_id, supplier_id,
        sheet_value, package_unit, freight_per_sheet, min_purchase_sheets, payment_days, delivery_days,
        price_date, preferred, state, effective_from, changed_by
    ) VALUES (
        NEW.company_id, NEW.paper_id, NEW.supplier_id,
        NEW.sheet_value, NEW.package_unit, NEW.freight_per_sheet, NEW.min_purchase_sheets, NEW.payment_days, NEW.delivery_days,
        NEW.price_date, NEW.preferred, NEW.state, now(), NULL
    );
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_paper_supplier_prices_log_history ON indicolors.paper_supplier_prices;
CREATE TRIGGER trg_paper_supplier_prices_log_history
    AFTER INSERT OR UPDATE ON indicolors.paper_supplier_prices
    FOR EACH ROW
    EXECUTE FUNCTION indicolors.fn_log_paper_price_history();

COMMENT ON FUNCTION indicolors.fn_log_paper_price_history() IS
    'Inserta un snapshot en paper_price_history al crear o actualizar un precio de papel';
