package com.inkcore.domain.platetype.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Agregado tipo de plancha. Campos alineados a {@code indicolors.plate_types}.
 */
public final class PlateType {

    private static final Set<String> ALLOWED_UNITS = Set.of("cm", "mm", "in");

    private final String plateTypeId;
    private final String companyId;
    private final String name;
    private final BigDecimal width;
    private final BigDecimal height;
    private final String unit;
    private final BigDecimal value;
    private final boolean state;
    private final LocalDate creationDate;

    private PlateType(
            String plateTypeId,
            String companyId,
            String name,
            BigDecimal width,
            BigDecimal height,
            String unit,
            BigDecimal value,
            boolean state,
            LocalDate creationDate
    ) {
        this.plateTypeId = plateTypeId;
        this.companyId = companyId;
        this.name = name;
        this.width = width;
        this.height = height;
        this.unit = unit;
        this.value = value;
        this.state = state;
        this.creationDate = creationDate;
    }

    public static PlateType createNew(
            String companyId,
            String name,
            BigDecimal width,
            BigDecimal height,
            String unit,
            BigDecimal value,
            boolean state,
            LocalDate creationDate
    ) {
        requireNotBlank(companyId, "La empresa es obligatoria");
        requireNotBlank(name, "El nombre es obligatorio");
        return new PlateType(
                UUID.randomUUID().toString(),
                companyId.trim(),
                name.trim(),
                normalizePositiveDimension(width, "El ancho debe ser mayor que 0"),
                normalizePositiveDimension(height, "El alto debe ser mayor que 0"),
                normalizeUnit(unit),
                normalizeNonNegativeMoney(value),
                state,
                creationDate
        );
    }

    public PlateType update(
            String name,
            BigDecimal width,
            BigDecimal height,
            String unit,
            BigDecimal value,
            boolean state
    ) {
        requireNotBlank(name, "El nombre es obligatorio");
        return new PlateType(
                this.plateTypeId,
                this.companyId,
                name.trim(),
                normalizePositiveDimension(width, "El ancho debe ser mayor que 0"),
                normalizePositiveDimension(height, "El alto debe ser mayor que 0"),
                normalizeUnit(unit),
                normalizeNonNegativeMoney(value),
                state,
                this.creationDate
        );
    }

    public static PlateType reconstitute(
            String plateTypeId,
            String companyId,
            String name,
            BigDecimal width,
            BigDecimal height,
            String unit,
            BigDecimal value,
            boolean state,
            LocalDate creationDate
    ) {
        return new PlateType(
                plateTypeId,
                companyId,
                name,
                width,
                height,
                unit,
                value,
                state,
                creationDate
        );
    }

    private static BigDecimal normalizePositiveDimension(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizeNonNegativeMoney(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("El valor es obligatorio");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El valor no puede ser negativo");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static String normalizeUnit(String unit) {
        String normalized = unit == null || unit.isBlank() ? "cm" : unit.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_UNITS.contains(normalized)) {
            throw new IllegalArgumentException("La unidad debe ser cm, mm o in");
        }
        return normalized;
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public String getPlateTypeId() {
        return plateTypeId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getWidth() {
        return width;
    }

    public BigDecimal getHeight() {
        return height;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getValue() {
        return value;
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
        if (!(o instanceof PlateType that)) {
            return false;
        }
        return Objects.equals(plateTypeId, that.plateTypeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plateTypeId);
    }
}
