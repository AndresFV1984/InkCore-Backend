-- Vista: CxC abiertas (saldo > 0). Fuente de listado cartera multi-documento.
-- Depende de: order_cxc_documents (V66), order_payment_applications (V67).

CREATE OR REPLACE VIEW indicolors.v_order_cxc_open AS
SELECT
    d.cxc_document_id,
    d.company_id,
    d.cxc_number,
    d.production_order_id,
    d.customer_order_id,
    d.client_id,
    d.concept,
    d.source_delivery_id,
    d.source_addition_id,
    d.total,
    d.status,
    d.issued_at,
    d.due_date,
    d.payment_term_days,
    COALESCE(SUM(a.amount) FILTER (WHERE a.status = 'activo'), 0)::NUMERIC(14,2) AS total_applied,
    (d.total - COALESCE(SUM(a.amount) FILTER (WHERE a.status = 'activo'), 0))::NUMERIC(14,2) AS remaining
FROM indicolors.order_cxc_documents d
LEFT JOIN indicolors.order_payment_applications a
    ON a.cxc_document_id = d.cxc_document_id
   AND a.company_id = d.company_id
WHERE d.status IN ('pendiente', 'parcial', 'pagada')
GROUP BY
    d.cxc_document_id,
    d.company_id,
    d.cxc_number,
    d.production_order_id,
    d.customer_order_id,
    d.client_id,
    d.concept,
    d.source_delivery_id,
    d.source_addition_id,
    d.total,
    d.status,
    d.issued_at,
    d.due_date,
    d.payment_term_days
HAVING (d.total - COALESCE(SUM(a.amount) FILTER (WHERE a.status = 'activo'), 0)) > 0;

COMMENT ON VIEW indicolors.v_order_cxc_open IS
    'CxC con saldo pendiente > 0. Listado Cuentas por cobrar multi-documento.';

GRANT SELECT ON indicolors.v_order_cxc_open TO indicolors_owner;
GRANT SELECT ON indicolors.v_order_cxc_open TO indicolors_app;
