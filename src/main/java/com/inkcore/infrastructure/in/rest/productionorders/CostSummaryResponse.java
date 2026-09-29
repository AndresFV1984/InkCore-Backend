package com.inkcore.infrastructure.in.rest.productionorders;

import com.inkcore.application.productionorder.usecase.GetProductionOrderCostSummaryUseCase;
import com.inkcore.domain.productionorder.model.CostSummary;
import com.inkcore.domain.productionorder.model.WasteRecord;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "ProductionOrderCostSummaryResponse")
public record CostSummaryResponse(
        String productionOrderId,
        String companyId,
        BigDecimal estimatedMaterialCost,
        BigDecimal estimatedMachineCost,
        BigDecimal estimatedWasteCost,
        @Schema(description = "Suma de planned_cost de las mermas. El desperdicio no entra aquí.")
        BigDecimal estimatedMermaCost,
        BigDecimal estimatedTotalCost,
        BigDecimal actualMaterialCost,
        BigDecimal actualMachineCost,
        @Schema(description = "Merma real más desperdicio. actual_total_cost lo incluye por esta columna.")
        BigDecimal actualWasteCost,
        @Schema(description = "Suma de la merma real. Si aún no hay cantidad real, usa el planificado.")
        BigDecimal actualMermaCost,
        @Schema(description = "Pérdida del taller por exceso o retrabajo. No modifica quotedPrice.")
        BigDecimal actualDesperdicioCost,
        BigDecimal actualTotalCost,
        @Schema(description = "totalToCharge de la OP (precio cotizado tras descuentos de cobro)", example = "1650000.00")
        BigDecimal quotedPrice,
        @Schema(description = "quotedPrice - estimatedTotalCost")
        BigDecimal estimatedMargin,
        @Schema(description = "quotedPrice - actualTotalCost")
        BigDecimal actualMargin,
        @Schema(description = "Margen real como porcentaje del precio cotizado", example = "-3.39")
        BigDecimal actualMarginPct,
        LocalDateTime updatedAt,
        @Schema(description = "Registros de desperdicio de la orden (exceso y retrabajo)")
        List<DesperdicioResponse> desperdicios
) {
    public static CostSummaryResponse from(GetProductionOrderCostSummaryUseCase.ProductionOrderCostRead read) {
        CostSummary summary = read.summary();
        return new CostSummaryResponse(
                summary.getProductionOrderId(),
                summary.getCompanyId(),
                summary.getEstimatedMaterialCost(),
                summary.getEstimatedMachineCost(),
                summary.getEstimatedWasteCost(),
                summary.getEstimatedMermaCost(),
                summary.getEstimatedTotalCost(),
                summary.getActualMaterialCost(),
                summary.getActualMachineCost(),
                summary.getActualWasteCost(),
                summary.getActualMermaCost(),
                summary.getActualDesperdicioCost(),
                summary.getActualTotalCost(),
                summary.getQuotedPrice(),
                summary.getEstimatedMargin(),
                summary.getActualMargin(),
                summary.getActualMarginPct(),
                summary.getUpdatedAt(),
                read.desperdicios().stream().map(DesperdicioResponse::from).toList()
        );
    }

    @Schema(name = "ProductionOrderDesperdicioResponse")
    public record DesperdicioResponse(
            @Schema(allowableValues = {"preprensa", "corte-papel", "impresion", "terminados", "acabados"})
            String phase,
            @Schema(allowableValues = {"exceso", "retrabajo"})
            String wasteOrigin,
            BigDecimal actualQuantity,
            BigDecimal actualCost,
            @Schema(description = "Etiqueta del motivo de la merma")
            String note
    ) {
        static DesperdicioResponse from(WasteRecord record) {
            return new DesperdicioResponse(
                    record.getPhase(),
                    record.getWasteOrigin(),
                    record.getActualQuantity(),
                    record.getActualCost(),
                    record.getNote()
            );
        }
    }
}
