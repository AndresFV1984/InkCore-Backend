package com.inkcore.domain.machine.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Catálogo de máquinas. {@code cost_per_hour} lo calcula la base (columna
 * generada); el dominio solo valida los insumos.
 */
public final class Machine {

    private final String machineId;
    private final String companyId;
    private final String name;
    private final MachineType machineType;
    private final String manufacturer;
    private final String model;
    private final BigDecimal purchaseCost;
    private final BigDecimal usefulLifeYears;
    private final BigDecimal annualMaintenanceCost;
    private final BigDecimal monthlyOperatorCost;
    private final BigDecimal energyCostPerHour;
    private final BigDecimal productiveHoursPerYear;
    private final BigDecimal costPerHour;
    private final boolean state;
    private final LocalDate creationDate;
    private final LocalDateTime updatedAt;

    private Machine(
            String machineId,
            String companyId,
            String name,
            MachineType machineType,
            String manufacturer,
            String model,
            BigDecimal purchaseCost,
            BigDecimal usefulLifeYears,
            BigDecimal annualMaintenanceCost,
            BigDecimal monthlyOperatorCost,
            BigDecimal energyCostPerHour,
            BigDecimal productiveHoursPerYear,
            BigDecimal costPerHour,
            boolean state,
            LocalDate creationDate,
            LocalDateTime updatedAt
    ) {
        this.machineId = machineId;
        this.companyId = companyId;
        this.name = name;
        this.machineType = machineType;
        this.manufacturer = manufacturer;
        this.model = model;
        this.purchaseCost = purchaseCost;
        this.usefulLifeYears = usefulLifeYears;
        this.annualMaintenanceCost = annualMaintenanceCost;
        this.monthlyOperatorCost = monthlyOperatorCost;
        this.energyCostPerHour = energyCostPerHour;
        this.productiveHoursPerYear = productiveHoursPerYear;
        this.costPerHour = costPerHour;
        this.state = state;
        this.creationDate = creationDate;
        this.updatedAt = updatedAt;
    }

    public static Machine createNew(
            String companyId,
            String name,
            MachineType machineType,
            String manufacturer,
            String model,
            BigDecimal purchaseCost,
            BigDecimal usefulLifeYears,
            BigDecimal annualMaintenanceCost,
            BigDecimal monthlyOperatorCost,
            BigDecimal energyCostPerHour,
            BigDecimal productiveHoursPerYear,
            boolean state,
            LocalDate creationDate,
            LocalDateTime updatedAt
    ) {
        requireNotBlank(companyId, "La empresa es obligatoria");
        requireNotBlank(name, "El nombre es obligatorio");
        if (machineType == null) {
            throw new IllegalArgumentException("El tipo de máquina es obligatorio");
        }
        return new Machine(
                UUID.randomUUID().toString(),
                companyId.trim(),
                name.trim(),
                machineType,
                blankToNull(manufacturer),
                blankToNull(model),
                nonNegativeMoney(purchaseCost, "El costo de adquisición es obligatorio y no puede ser negativo"),
                positive(usefulLifeYears, "La vida útil debe ser mayor que 0"),
                nonNegativeMoney(zeroIfNull(annualMaintenanceCost), "El mantenimiento anual no puede ser negativo"),
                nonNegativeMoney(zeroIfNull(monthlyOperatorCost), "El costo mensual del operario no puede ser negativo"),
                nonNegativeMoney(zeroIfNull(energyCostPerHour), "El costo de energía por hora no puede ser negativo"),
                positive(productiveHoursPerYear, "Las horas productivas al año deben ser mayores que 0"),
                null,
                state,
                creationDate,
                updatedAt
        );
    }

    public Machine update(
            String name,
            MachineType machineType,
            String manufacturer,
            String model,
            BigDecimal purchaseCost,
            BigDecimal usefulLifeYears,
            BigDecimal annualMaintenanceCost,
            BigDecimal monthlyOperatorCost,
            BigDecimal energyCostPerHour,
            BigDecimal productiveHoursPerYear,
            boolean state,
            LocalDateTime updatedAt
    ) {
        requireNotBlank(name, "El nombre es obligatorio");
        if (machineType == null) {
            throw new IllegalArgumentException("El tipo de máquina es obligatorio");
        }
        return new Machine(
                this.machineId,
                this.companyId,
                name.trim(),
                machineType,
                blankToNull(manufacturer),
                blankToNull(model),
                nonNegativeMoney(purchaseCost, "El costo de adquisición es obligatorio y no puede ser negativo"),
                positive(usefulLifeYears, "La vida útil debe ser mayor que 0"),
                nonNegativeMoney(zeroIfNull(annualMaintenanceCost), "El mantenimiento anual no puede ser negativo"),
                nonNegativeMoney(zeroIfNull(monthlyOperatorCost), "El costo mensual del operario no puede ser negativo"),
                nonNegativeMoney(zeroIfNull(energyCostPerHour), "El costo de energía por hora no puede ser negativo"),
                positive(productiveHoursPerYear, "Las horas productivas al año deben ser mayores que 0"),
                this.costPerHour,
                state,
                this.creationDate,
                updatedAt
        );
    }

    public static Machine reconstitute(
            String machineId,
            String companyId,
            String name,
            MachineType machineType,
            String manufacturer,
            String model,
            BigDecimal purchaseCost,
            BigDecimal usefulLifeYears,
            BigDecimal annualMaintenanceCost,
            BigDecimal monthlyOperatorCost,
            BigDecimal energyCostPerHour,
            BigDecimal productiveHoursPerYear,
            BigDecimal costPerHour,
            boolean state,
            LocalDate creationDate,
            LocalDateTime updatedAt
    ) {
        return new Machine(
                machineId,
                companyId,
                name,
                machineType,
                manufacturer,
                model,
                purchaseCost,
                usefulLifeYears,
                annualMaintenanceCost,
                monthlyOperatorCost,
                energyCostPerHour,
                productiveHoursPerYear,
                costPerHour,
                state,
                creationDate,
                updatedAt
        );
    }

    private static BigDecimal nonNegativeMoney(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(message);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal positive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public String getMachineId() {
        return machineId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public MachineType getMachineType() {
        return machineType;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public BigDecimal getPurchaseCost() {
        return purchaseCost;
    }

    public BigDecimal getUsefulLifeYears() {
        return usefulLifeYears;
    }

    public BigDecimal getAnnualMaintenanceCost() {
        return annualMaintenanceCost;
    }

    public BigDecimal getMonthlyOperatorCost() {
        return monthlyOperatorCost;
    }

    public BigDecimal getEnergyCostPerHour() {
        return energyCostPerHour;
    }

    public BigDecimal getProductiveHoursPerYear() {
        return productiveHoursPerYear;
    }

    public BigDecimal getCostPerHour() {
        return costPerHour;
    }

    public boolean isState() {
        return state;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Machine that)) {
            return false;
        }
        return Objects.equals(machineId, that.machineId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(machineId);
    }
}
