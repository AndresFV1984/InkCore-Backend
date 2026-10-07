package com.inkcore.application.paper.usecase;

import java.math.BigDecimal;

public record CreatePaperCommand(
        String name,
        BigDecimal grammage,
        BigDecimal width,
        BigDecimal height,
        String unit,
        Boolean coated,
        Boolean acceptsRemnants,
        BigDecimal minRemnantWidth,
        BigDecimal minRemnantHeight,
        String minRemnantUnit,
        Boolean state
) {
}
