package com.inkcore.application.finish.usecase;

import java.math.BigDecimal;

public record CreateFinishCommand(
        String companyId,
        String name,
        BigDecimal minCost,
        BigDecimal valuePerCm2,
        Boolean quickAccess,
        Boolean state
) {
}
