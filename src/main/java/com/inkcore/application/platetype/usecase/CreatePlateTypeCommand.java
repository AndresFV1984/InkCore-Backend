package com.inkcore.application.platetype.usecase;

import java.math.BigDecimal;

public record CreatePlateTypeCommand(
        String companyId,
        String name,
        BigDecimal width,
        BigDecimal height,
        String unit,
        BigDecimal value,
        Boolean state
) {
}
