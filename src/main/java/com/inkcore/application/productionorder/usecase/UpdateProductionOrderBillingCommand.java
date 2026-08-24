package com.inkcore.application.productionorder.usecase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record UpdateProductionOrderBillingCommand(
        Long version,
        String billingDiscountType,
        BigDecimal billingDiscountValue,
        String clientCostingMode,
        String clientDiscountType,
        BigDecimal clientDiscountValue,
        String clientProfitabilityType,
        BigDecimal clientProfitabilityValue,
        List<Map<String, Object>> clientVolumeCosting,
        LocalDate deliveryStartDate,
        LocalDate deliveryEndDate,
        BigDecimal advancePercentage,
        String clientSignatureName,
        String bankAccountId,
        Boolean completed,
        String operatorUserId
) {
}
