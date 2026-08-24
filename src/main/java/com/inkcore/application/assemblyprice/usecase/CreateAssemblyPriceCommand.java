package com.inkcore.application.assemblyprice.usecase;

import java.math.BigDecimal;

public record CreateAssemblyPriceCommand(
        String companyId,
        String name,
        BigDecimal cost,
        Boolean state
) {
}
