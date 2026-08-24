package com.inkcore.domain.productionorder.ports.out;

import com.inkcore.domain.productionorder.model.ProductionOrder;

import java.math.BigDecimal;

/**
 * Generación del PDF de cobro de la Orden de Producción.
 */
public interface ProductionOrderPdfPort {

    byte[] generateBillingPdf(ProductionOrder order, String clientName, BigDecimal total);
}
