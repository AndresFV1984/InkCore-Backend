package com.inkcore.application.machine.usecase;

import com.inkcore.domain.machine.model.MachineType;

import java.math.BigDecimal;

public record CreateMachineCommand(
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
        Boolean state
) {
    public MachineType type() {
        return MachineType.fromApiValue(machineType);
    }
}
