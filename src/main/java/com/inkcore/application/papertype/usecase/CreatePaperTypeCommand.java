package com.inkcore.application.papertype.usecase;

import java.math.BigDecimal;
import java.util.List;

public record CreatePaperTypeCommand(
        String companyId,
        String name,
        BigDecimal width,
        BigDecimal height,
        String unit,
        Boolean coated,
        Boolean state,
        List<PaperTypeCutAssignmentCommand> cutLayouts,
        List<PaperTypeSupplierAssignmentCommand> suppliers
) {
}
