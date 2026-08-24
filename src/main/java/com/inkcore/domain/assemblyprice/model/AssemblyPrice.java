package com.inkcore.domain.assemblyprice.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado precio de montaje. Campos alineados a {@code indicolors.assembly_prices}.
 */
public final class AssemblyPrice {

    private final String assemblyPriceId;
    private final String companyId;
    private final String name;
    private final BigDecimal cost;
    private final boolean state;
    private final LocalDate creationDate;

    private AssemblyPrice(
            String assemblyPriceId,
            String companyId,
            String name,
            BigDecimal cost,
            boolean state,
            LocalDate creationDate
    ) {
        this.assemblyPriceId = assemblyPriceId;
        this.companyId = companyId;
        this.name = name;
        this.cost = cost;
        this.state = state;
        this.creationDate = creationDate;
    }

    public static AssemblyPrice createNew(
            String companyId,
            String name,
            BigDecimal cost,
            boolean state,
            LocalDate creationDate
    ) {
        requireNotBlank(companyId, "La empresa es obligatoria");
        requireNotBlank(name, "El nombre es obligatorio");
        return new AssemblyPrice(
                UUID.randomUUID().toString(),
                companyId.trim(),
                name.trim(),
                normalizeNonNegativeCost(cost),
                state,
                creationDate
        );
    }

    public AssemblyPrice update(String name, BigDecimal cost, boolean state) {
        requireNotBlank(name, "El nombre es obligatorio");
        return new AssemblyPrice(
                this.assemblyPriceId,
                this.companyId,
                name.trim(),
                normalizeNonNegativeCost(cost),
                state,
                this.creationDate
        );
    }

    public static AssemblyPrice reconstitute(
            String assemblyPriceId,
            String companyId,
            String name,
            BigDecimal cost,
            boolean state,
            LocalDate creationDate
    ) {
        return new AssemblyPrice(
                assemblyPriceId,
                companyId,
                name,
                cost,
                state,
                creationDate
        );
    }

    private static BigDecimal normalizeNonNegativeCost(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("El costo es obligatorio");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El costo no puede ser negativo");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public String getAssemblyPriceId() {
        return assemblyPriceId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public boolean isState() {
        return state;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AssemblyPrice that)) {
            return false;
        }
        return Objects.equals(assemblyPriceId, that.assemblyPriceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assemblyPriceId);
    }
}
