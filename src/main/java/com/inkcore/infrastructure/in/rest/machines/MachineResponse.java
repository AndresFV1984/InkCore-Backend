package com.inkcore.infrastructure.in.rest.machines;

import com.inkcore.domain.machine.model.Machine;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(name = "MachineResponse")
public record MachineResponse(
        @Schema(example = "814ad646-c4fe-42fa-9f13-4a44823e6bee")
        String machineId,
        @Schema(example = "company-seed-001")
        String companyId,
        @Schema(example = "Offset Heidelberg 4 colores")
        String name,
        @Schema(allowableValues = {"preprensa", "corte-papel", "impresion", "terminados", "acabados"}, example = "impresion")
        String machineType,
        @Schema(example = "Heidelberg")
        String manufacturer,
        @Schema(example = "SM 74")
        String model,
        @Schema(example = "250000000.00")
        BigDecimal purchaseCost,
        @Schema(example = "10.00")
        BigDecimal usefulLifeYears,
        @Schema(example = "8000000.00")
        BigDecimal annualMaintenanceCost,
        @Schema(example = "2500000.00")
        BigDecimal monthlyOperatorCost,
        @Schema(example = "15000.00")
        BigDecimal energyCostPerHour,
        @Schema(example = "1600.00")
        BigDecimal productiveHoursPerYear,
        @Schema(description = "Calculado por la base. El backend no lo escribe.", example = "54375.00")
        BigDecimal costPerHour,
        @Schema(description = "true=activa, false=inactiva", example = "true")
        boolean state,
        LocalDate creationDate,
        LocalDateTime updatedAt
) {
    public static MachineResponse from(Machine machine) {
        return new MachineResponse(
                machine.getMachineId(),
                machine.getCompanyId(),
                machine.getName(),
                machine.getMachineType().getApiValue(),
                machine.getManufacturer(),
                machine.getModel(),
                machine.getPurchaseCost(),
                machine.getUsefulLifeYears(),
                machine.getAnnualMaintenanceCost(),
                machine.getMonthlyOperatorCost(),
                machine.getEnergyCostPerHour(),
                machine.getProductiveHoursPerYear(),
                machine.getCostPerHour(),
                machine.isState(),
                machine.getCreationDate(),
                machine.getUpdatedAt()
        );
    }
}
