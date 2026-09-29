package com.inkcore.infrastructure.in.rest.reports;

import com.inkcore.domain.productionorder.model.ProfitabilityRow;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "ProfitabilityReportItem")
public record ProfitabilityReportItem(
        @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String productionOrderId,
        @Schema(example = "OP-42")
        String orderNumber,
        @Schema(example = "client-seed-001")
        String clientId,
        @Schema(example = "seller-seed-001")
        String sellerId,
        @Schema(example = "Brochure corporativo")
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
        @Schema(description = "Precio cotizado (totalToCharge)")
        BigDecimal quotedPrice,
        BigDecimal estimatedMargin,
        @Schema(description = "quotedPrice - actualTotalCost. Negativo = la orden deja pérdida.")
        BigDecimal actualMargin,
        @Schema(description = "Margen real como porcentaje del precio cotizado", example = "-3.39")
        BigDecimal actualMarginPct,
        @Schema(description = "Suma de planned_cost de las mermas. El desperdicio no entra aquí.")
        BigDecimal estimatedMermaCost,
        @Schema(description = "Suma de la merma real. Si aún no hay cantidad real, usa el planificado.")
        BigDecimal actualMermaCost,
        @Schema(description = "Pérdida del taller por exceso o retrabajo. No modifica quotedPrice.")
        BigDecimal actualDesperdicioCost
) {
    public static ProfitabilityReportItem from(ProfitabilityRow row) {
        return new ProfitabilityReportItem(
                row.productionOrderId(),
                row.orderNumber(),
                row.clientId(),
                row.sellerId(),
                row.workName(),
                row.orderDate(),
                row.estimatedMaterialCost(),
                row.estimatedMachineCost(),
                row.estimatedWasteCost(),
                row.estimatedTotalCost(),
                row.actualMaterialCost(),
                row.actualMachineCost(),
                row.actualWasteCost(),
                row.actualTotalCost(),
                row.quotedPrice(),
                row.estimatedMargin(),
                row.actualMargin(),
                row.actualMarginPct(),
                row.estimatedMermaCost(),
                row.actualMermaCost(),
                row.actualDesperdicioCost()
        );
    }
}
