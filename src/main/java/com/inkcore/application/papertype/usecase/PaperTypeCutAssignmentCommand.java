package com.inkcore.application.papertype.usecase;

import java.math.BigDecimal;
import java.util.List;

public record PaperTypeCutAssignmentCommand(
        String cutLayoutId,
        BigDecimal cutValue
) {
}
