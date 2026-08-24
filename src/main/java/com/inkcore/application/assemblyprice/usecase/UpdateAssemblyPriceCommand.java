package com.inkcore.application.assemblyprice.usecase;

import java.math.BigDecimal;

public record UpdateAssemblyPriceCommand(
        String assemblyPriceId,
        String name,
        BigDecimal cost,
        boolean state
) {
}
