package com.inkcore.application.thousandrate.usecase;

import java.math.BigDecimal;

public record UpdateThousandRateCommand(
        String thousandRateId,
        String name,
        String colorCategory,
        Integer thousandUnit,
        BigDecimal price,
        boolean state,
        Integer minThresholdUnits,
        BigDecimal minThousand,
        BigDecimal decimalThreshold,
        BigDecimal gripperFlipPrice,
        BigDecimal squareFlipPrice,
        boolean isDefault
) {
}
