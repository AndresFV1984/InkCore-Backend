package com.inkcore.application.finish.usecase;

import java.math.BigDecimal;

public record UpdateFinishCommand(
        String finishId,
        String name,
        BigDecimal minCost,
        BigDecimal valuePerCm2,
        boolean quickAccess,
        boolean state
) {
}
