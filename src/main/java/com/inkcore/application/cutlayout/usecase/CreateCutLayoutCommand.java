package com.inkcore.application.cutlayout.usecase;

import java.math.BigDecimal;

public record CreateCutLayoutCommand(
        String companyId,
        String name,
        BigDecimal width,
        BigDecimal height,
        String unit,
        Integer piecesPerSheet,
        Boolean state
) {
}
