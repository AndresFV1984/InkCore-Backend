package com.inkcore.application.papertype.usecase;

import java.math.BigDecimal;

public record PaperTypeSupplierAssignmentCommand(
        String supplierId,
        BigDecimal sheetValue,
        Integer packageUnit
) {
}
