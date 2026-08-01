package com.inkcore.application.cutlayout.usecase;

import java.math.BigDecimal;

public record UpdateCutLayoutCommand(
        String cutLayoutId,
        String name,
        BigDecimal width,
        BigDecimal height,
        String unit,
        int piecesPerSheet,
        boolean state
) {
}
