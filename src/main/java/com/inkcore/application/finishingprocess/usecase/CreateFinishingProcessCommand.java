package com.inkcore.application.finishingprocess.usecase;

import java.math.BigDecimal;

public record CreateFinishingProcessCommand(
        String companyId,
        String name,
        BigDecimal minCost,
        BigDecimal valuePerCm2,
        Boolean quickAccess,
        Boolean state
) {
}
