package com.inkcore.application.finishingprocess.usecase;

import java.math.BigDecimal;

public record UpdateFinishingProcessCommand(
        String finishingProcessId,
        String name,
        BigDecimal minCost,
        BigDecimal valuePerCm2,
        boolean quickAccess,
        boolean state
) {
}
