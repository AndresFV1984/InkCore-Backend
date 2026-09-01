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
        List<PaperRowInput> paperRows
) {
    public record PaperRowInput(
            String paperRowId,
            String plateId,
            String parentRowId,
            String cutRowKey,
            Boolean isMissingSupply,
            Integer missingSheetsQuantity,
            Boolean clientSuppliesPaper,
            String paperTypeId,
            String supplierId,
            String cutLayoutId,
            Boolean isPaperCut,
            Integer deliveredSheetsByClient,
            Integer manualGoodSizes,
            Integer manualSurplus
    ) {
    }
}
