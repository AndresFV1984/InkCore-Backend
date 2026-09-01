package com.inkcore.application.papertype.usecase;

import java.math.BigDecimal;
import java.util.List;

public record UpdatePaperTypeCommand(
        String paperTypeId,
        String name,
        BigDecimal width,
        BigDecimal height,
        String unit,
        boolean coated,
        boolean state,
        List<PaperTypeCutAssignmentCommand> cutLayouts,
        List<PaperTypeSupplierAssignmentCommand> suppliers
) {
}
