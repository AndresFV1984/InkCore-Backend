package com.inkcore.domain.productionorder.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fila del reporte de rentabilidad: resumen de costos de la OP más datos
 * comerciales para filtrar por cliente y vendedor.
 */
public record ProfitabilityRow(
        String productionOrderId,
        String orderNumber,
        String clientId,
        String sellerId,
        String workName,
        LocalDate orderDate,
        BigDecimal estimatedMaterialCost,
        BigDecimal estimatedMachineCost,
        BigDecimal estimatedWasteCost,
        BigDecimal estimatedTotalCost,
        BigDecimal actualMaterialCost,
        BigDecimal actualMachineCost,
        BigDecimal actualWasteCost,
        BigDecimal actualTotalCost,
        BigDecimal quotedPrice,
        BigDecimal estimatedMargin,
        BigDecimal actualMargin,
        BigDecimal actualMarginPct,
        BigDecimal estimatedMermaCost,
        BigDecimal actualMermaCost,
        BigDecimal actualDesperdicioCost
) {
}
