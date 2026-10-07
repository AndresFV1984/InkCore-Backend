package com.inkcore.application.productionorder.usecase;

import java.math.BigDecimal;
import java.util.List;

public record UpdateProductionOrderPaperCuttingCommand(
        Long version,
        Boolean clientSuppliesPaperDefault,
        Integer roundingMargin,
        Boolean completed,
        List<OperatorAssignmentCommand> operators,
        String operatorUserId,
        String discountType,
        BigDecimal discountValue,
        List<PaperRowInput> paperRows,
        List<MachineUsageInput> machineUsages,
        BigDecimal plannedMakereadyQuantity
) {
    /**
     * @param paperCutLayoutId  si viene, resuelve cutLayoutId desde paper_cut_layouts
     * @param priceRule         PREFERRED | REPLACEMENT | BEST_COST (opcional)
     */
    public record PaperRowInput(
            String paperRowId,
            String plateId,
            String parentRowId,
            String cutRowKey,
            Boolean isMissingSupply,
            Integer missingSheetsQuantity,
            Boolean clientSuppliesPaper,
            String paperId,
            String supplierId,
            String cutLayoutId,
            String paperCutLayoutId,
            String priceRule,
            Boolean isPaperCut,
            Integer deliveredSheetsByClient,
            Integer manualGoodSizes,
            Integer manualSurplus,
            BigDecimal plannedWastePercentage,
            /** Remanente usado como origen del corte (descuenta quantity_available). */
            String paperRemnantId
    ) {
    }
}
