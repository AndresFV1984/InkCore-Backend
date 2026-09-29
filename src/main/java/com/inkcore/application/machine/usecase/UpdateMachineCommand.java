package com.inkcore.application.machine.usecase;

import java.math.BigDecimal;

public record UpdateMachineCommand(
        String machineId,
        String name,
        String machineType,
        String manufacturer,
        String model,
        BigDecimal purchaseCost,
        BigDecimal usefulLifeYears,
        BigDecimal annualMaintenanceCost,
        BigDecimal monthlyOperatorCost,
        BigDecimal energyCostPerHour,
        BigDecimal productiveHoursPerYear,
        boolean state
) {
}
