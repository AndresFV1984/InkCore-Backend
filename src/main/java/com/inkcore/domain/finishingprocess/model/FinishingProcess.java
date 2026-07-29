package com.inkcore.domain.finishingprocess.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado proceso de acabado. Campos alineados a {@code indicolors.finishing_processes}.
 */
public final class FinishingProcess {

    private static final BigDecimal VALUE_PER_CM2_MAX = new BigDecimal("9999");

    private final String finishingProcessId;
    private final String companyId;
    private final String name;
    private final BigDecimal minCost;
    private final BigDecimal valuePerCm2;
    private final boolean quickAccess;
    private final boolean state;
    private final LocalDate creationDate;

    private FinishingProcess(
            String finishingProcessId,
            String companyId,
            String name,
            BigDecimal minCost,
            BigDecimal valuePerCm2,
            boolean quickAccess,
            boolean state,
            LocalDate creationDate
    ) {
        this.finishingProcessId = finishingProcessId;
        this.companyId = companyId;
        this.name = name;
        this.minCost = minCost;
        this.valuePerCm2 = valuePerCm2;
        this.quickAccess = quickAccess;
        this.state = state;
        this.creationDate = creationDate;
    }

    public static FinishingProcess createNew(
            String companyId,
            String name,
            BigDecimal minCost,
            BigDecimal valuePerCm2,
            boolean quickAccess,
            boolean state,
            LocalDate creationDate
    ) {
        requireNotBlank(companyId, "La empresa es obligatoria");
        requireNotBlank(name, "El nombre es obligatorio");

        return new FinishingProcess(
                UUID.randomUUID().toString(),
                companyId.trim(),
                name.trim(),
                normalizeMinCost(minCost),
                normalizeValuePerCm2(valuePerCm2),
                quickAccess,
                state,
                creationDate
        );
    }

    public FinishingProcess update(
            String name,
            BigDecimal minCost,
            BigDecimal valuePerCm2,
            boolean quickAccess,
            boolean state
    ) {
        requireNotBlank(name, "El nombre es obligatorio");
        return new FinishingProcess(
                this.finishingProcessId,
                this.companyId,
                name.trim(),
                normalizeMinCost(minCost),
                normalizeValuePerCm2(valuePerCm2),
                quickAccess,
                state,
                this.creationDate
        );
    }

    public static FinishingProcess reconstitute(
            String finishingProcessId,
            String companyId,
            String name,
            BigDecimal minCost,
            BigDecimal valuePerCm2,
            boolean quickAccess,
            boolean state,
            LocalDate creationDate
    ) {
        return new FinishingProcess(
                finishingProcessId,
                companyId,
                name,
                minCost,
                valuePerCm2,
                quickAccess,
                state,
                creationDate
        );
    }

    private static BigDecimal normalizeMinCost(BigDecimal minCost) {
        if (minCost == null) {
            return null;
        }
        if (minCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El costo mínimo no puede ser negativo");
        }
        return minCost.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizeValuePerCm2(BigDecimal valuePerCm2) {
        BigDecimal value = valuePerCm2 == null ? BigDecimal.ZERO : valuePerCm2;
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(VALUE_PER_CM2_MAX) > 0) {
            throw new IllegalArgumentException("El valor cm² debe estar entre 0 y 9999");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public String getFinishingProcessId() {
        return finishingProcessId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getMinCost() {
        return minCost;
    }

    public BigDecimal getValuePerCm2() {
        return valuePerCm2;
    }

    public boolean isQuickAccess() {
        return quickAccess;
    }

    public boolean isState() {
        return state;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FinishingProcess that)) return false;
        return Objects.equals(finishingProcessId, that.finishingProcessId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(finishingProcessId);
    }
}
