package com.inkcore.application.productionorder.usecase;

import java.math.BigDecimal;
import java.util.List;

public record UpdateProductionOrderPrepressCommand(
        Long version,
        Boolean isNewDesign,
        String designName,
        String existingDesignOrderId,
        Boolean hasDesignCost,
        BigDecimal designCost,
        Boolean clientSuppliesPlates,
        String clientPlateType,
        BigDecimal newPlateCost,
        String assemblyPriceId,
        Boolean dieCutLine,
        Boolean uvReserve,
        Boolean stamping,
        Boolean embossing,
        String prepressDiscountType,
        BigDecimal prepressDiscountValue,
        Boolean completed,
        String operatorUserId,
        List<PlateInput> plates
) {
    public record PlateInput(
            String plateId,
            String colors,
            String plateTypeId,
            Integer quantity,
            Integer cavities,
            Integer surplus,
            Integer platesCount,
            String detail,
            String observation,
            Boolean manualEntry,
            String plateSupply,
            Boolean plateReplacement,
            Integer replacementQuantity,
            String plateName,
            String plateSize,
            BigDecimal platePrice
    ) {
    }
}
