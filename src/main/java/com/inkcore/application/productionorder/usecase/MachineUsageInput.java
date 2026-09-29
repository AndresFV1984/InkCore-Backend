package com.inkcore.application.productionorder.usecase;

import java.math.BigDecimal;

public record MachineUsageInput(
        String machineId,
        Integer estimatedSetupMinutes,
        Integer estimatedRunMinutes
) {
}
