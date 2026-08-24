package com.inkcore.application.productionorder.usecase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record UpdateProductionOrderPrintingCommand(
        Long version,
        Boolean completed,
        String operatorUserId,
        List<PrintInput> prints
) {
    public record PrintInput(
            String printId,
            String plateId,
            Boolean clientSuppliesSherpa,
            BigDecimal sherpaTestPrice,
            BigDecimal machineOutputValue,
            Map<String, Object> inkEstimation,
            String printingDiscountType,
            BigDecimal printingDiscountValue,
            Boolean completed,
            List<PrintEntryInput> entries
    ) {
    }

    public record PrintEntryInput(
            String printEntryId,
            Integer shotsInkCount,
            List<String> shotsInks,
            Integer reverseInkCount,
            List<String> reverseInks,
            String basicFlipType,
            String basicThousandRateId,
            String pantoneFlipType,
            Boolean clientSuppliesPantoneInk,
            BigDecimal pantoneInkChargePrice,
            String pantoneThousandRateId
    ) {
    }
}
