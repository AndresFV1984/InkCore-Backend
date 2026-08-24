package com.inkcore.application.thousandrate.usecase;

import java.math.BigDecimal;

public record CreateThousandRateCommand(
        String companyId,
        String name,
        String colorCategory,
        Integer thousandUnit,
        BigDecimal price,
        Boolean state,
        Integer minThresholdUnits,
        BigDecimal minThousand,
        BigDecimal decimalThreshold,
        BigDecimal gripperFlipPrice,
        BigDecimal squareFlipPrice,
        Boolean isDefault
) {
}
