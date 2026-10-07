package com.inkcore.domain.paper.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Agregado de papel del catálogo: material + formato de compra (width/height/unit)
 * y política opcional de remanentes reutilizables (mínimas con unidad propia).
 */
public final class Paper {

    private static final Set<String> ALLOWED_UNITS = Set.of("cm", "mm", "in");
    private static final BigDecimal MM_PER_CM = new BigDecimal("10");
    private static final BigDecimal MM_PER_IN = new BigDecimal("25.4");

    private final String paperId;
    private final String companyId;
    private final String name;
    private final BigDecimal grammage;
    private final BigDecimal width;
    private final BigDecimal height;
    private final String unit;
    private final boolean coated;
    private final boolean acceptsRemnants;
    private final BigDecimal minRemnantWidth;
    private final BigDecimal minRemnantHeight;
    private final String minRemnantUnit;
    private final boolean state;
    private final LocalDate creationDate;
    private final LocalDateTime updatedAt;

    private Paper(
            String paperId,
            String companyId,
            String name,
            BigDecimal grammage,
            BigDecimal width,
            BigDecimal height,
            String unit,
            boolean coated,
            boolean acceptsRemnants,
            BigDecimal minRemnantWidth,
            BigDecimal minRemnantHeight,
            String minRemnantUnit,
            boolean state,
            LocalDate creationDate,
            LocalDateTime updatedAt
    ) {
        this.paperId = paperId;
        this.companyId = companyId;
        this.name = name;
        this.grammage = grammage;
        this.width = width;
        this.height = height;
        this.unit = unit;
        this.coated = coated;
        this.acceptsRemnants = acceptsRemnants;
        this.minRemnantWidth = minRemnantWidth;
        this.minRemnantHeight = minRemnantHeight;
        this.minRemnantUnit = minRemnantUnit;
        this.state = state;
        this.creationDate = creationDate;
        this.updatedAt = updatedAt;
    }

    public static Paper createNew(
            String companyId,
            String name,
            BigDecimal grammage,
            BigDecimal width,
            BigDecimal height,
            String unit,
            boolean coated,
            Boolean acceptsRemnants,
            BigDecimal minRemnantWidth,
            BigDecimal minRemnantHeight,
            String minRemnantUnit,
            boolean state,
            LocalDate creationDate,
            LocalDateTime updatedAt
    ) {
        requireNotBlank(companyId, "La empresa es obligatoria");
        requireNotBlank(name, "El nombre es obligatorio");
        BigDecimal normalizedWidth = normalizePositiveDimension(width, "El ancho debe ser mayor que 0");
        BigDecimal normalizedHeight = normalizePositiveDimension(height, "El alto debe ser mayor que 0");
        String normalizedUnit = normalizeUnit(unit);
        RemnantPolicy policy = RemnantPolicy.of(
                acceptsRemnants,
                minRemnantWidth,
                minRemnantHeight,
                minRemnantUnit,
                normalizedWidth,
                normalizedHeight,
                normalizedUnit
        );
        return new Paper(
                UUID.randomUUID().toString(),
                companyId.trim(),
                name.trim(),
                normalizeGrammage(grammage),
                normalizedWidth,
                normalizedHeight,
                normalizedUnit,
                coated,
                policy.acceptsRemnants(),
                policy.minRemnantWidth(),
                policy.minRemnantHeight(),
                policy.minRemnantUnit(),
                state,
                creationDate,
                updatedAt
        );
    }

    public Paper update(
            String name,
            BigDecimal grammage,
            BigDecimal width,
            BigDecimal height,
            String unit,
            boolean coated,
            Boolean acceptsRemnants,
            BigDecimal minRemnantWidth,
            BigDecimal minRemnantHeight,
            String minRemnantUnit,
            boolean state,
            LocalDateTime updatedAt
    ) {
        requireNotBlank(name, "El nombre es obligatorio");
        BigDecimal normalizedWidth = normalizePositiveDimension(width, "El ancho debe ser mayor que 0");
        BigDecimal normalizedHeight = normalizePositiveDimension(height, "El alto debe ser mayor que 0");
        String normalizedUnit = normalizeUnit(unit);
        boolean accepts = acceptsRemnants == null ? this.acceptsRemnants : acceptsRemnants;
        BigDecimal minW;
        BigDecimal minH;
        String minUnit;
        if (!accepts) {
            minW = null;
            minH = null;
            minUnit = null;
        } else {
            minW = minRemnantWidth != null ? minRemnantWidth : this.minRemnantWidth;
            minH = minRemnantHeight != null ? minRemnantHeight : this.minRemnantHeight;
            minUnit = minRemnantUnit != null && !minRemnantUnit.isBlank()
                    ? minRemnantUnit
                    : this.minRemnantUnit;
        }
        RemnantPolicy policy = RemnantPolicy.of(
                accepts, minW, minH, minUnit, normalizedWidth, normalizedHeight, normalizedUnit);
        return new Paper(
                this.paperId,
                this.companyId,
                name.trim(),
                normalizeGrammage(grammage),
                normalizedWidth,
                normalizedHeight,
                normalizedUnit,
                coated,
                policy.acceptsRemnants(),
                policy.minRemnantWidth(),
                policy.minRemnantHeight(),
                policy.minRemnantUnit(),
                state,
                this.creationDate,
                updatedAt
        );
    }

    public static Paper reconstitute(
            String paperId,
            String companyId,
            String name,
            BigDecimal grammage,
            BigDecimal width,
            BigDecimal height,
            String unit,
            boolean coated,
            boolean acceptsRemnants,
            BigDecimal minRemnantWidth,
            BigDecimal minRemnantHeight,
            String minRemnantUnit,
            boolean state,
            LocalDate creationDate,
            LocalDateTime updatedAt
    ) {
        return new Paper(
                paperId, companyId, name, grammage, width, height, unit, coated,
                acceptsRemnants, minRemnantWidth, minRemnantHeight, minRemnantUnit,
                state, creationDate, updatedAt);
    }

    public static String normalizeUnit(String unit) {
        String normalized = unit == null || unit.isBlank() ? "cm" : unit.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_UNITS.contains(normalized)) {
            throw new IllegalArgumentException("La unidad debe ser cm, mm o in");
        }
        return normalized;
    }

    public static BigDecimal normalizePositiveDimension(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizeGrammage(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El gramaje debe ser mayor que 0");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    static BigDecimal toMillimeters(BigDecimal value, String unit) {
        return switch (unit) {
            case "mm" -> value;
            case "cm" -> value.multiply(MM_PER_CM);
            case "in" -> value.multiply(MM_PER_IN);
            default -> throw new IllegalArgumentException("La unidad debe ser cm, mm o in");
        };
    }

    private record RemnantPolicy(
            boolean acceptsRemnants,
            BigDecimal minRemnantWidth,
            BigDecimal minRemnantHeight,
            String minRemnantUnit
    ) {
        static RemnantPolicy of(
                Boolean acceptsRemnants,
                BigDecimal minRemnantWidth,
                BigDecimal minRemnantHeight,
                String minRemnantUnit,
                BigDecimal paperWidth,
                BigDecimal paperHeight,
                String paperUnit
        ) {
            boolean accepts = acceptsRemnants != null && acceptsRemnants;
            if (!accepts) {
                if (minRemnantWidth != null || minRemnantHeight != null
                        || (minRemnantUnit != null && !minRemnantUnit.isBlank())) {
                    throw new IllegalArgumentException(
                            "Si el papel no acepta remanentes, no envíe medidas ni unidad mínimas de remanente");
                }
                return new RemnantPolicy(false, null, null, null);
            }
            if (minRemnantWidth == null || minRemnantHeight == null) {
                throw new IllegalArgumentException(
                        "Si acceptsRemnants=true, minRemnantWidth y minRemnantHeight son obligatorios");
            }
            String remnantUnit = (minRemnantUnit == null || minRemnantUnit.isBlank())
                    ? paperUnit
                    : normalizeUnit(minRemnantUnit);
            BigDecimal minW = normalizePositiveDimension(
                    minRemnantWidth, "El ancho mínimo de remanente debe ser mayor que 0");
            BigDecimal minH = normalizePositiveDimension(
                    minRemnantHeight, "El alto mínimo de remanente debe ser mayor que 0");
            BigDecimal minWMm = toMillimeters(minW, remnantUnit);
            BigDecimal minHMm = toMillimeters(minH, remnantUnit);
            BigDecimal paperWMm = toMillimeters(paperWidth, paperUnit);
            BigDecimal paperHMm = toMillimeters(paperHeight, paperUnit);
            boolean fits = (minWMm.compareTo(paperWMm) <= 0 && minHMm.compareTo(paperHMm) <= 0)
                    || (minWMm.compareTo(paperHMm) <= 0 && minHMm.compareTo(paperWMm) <= 0);
            if (!fits) {
                throw new IllegalArgumentException(
                        "Las medidas mínimas de remanente no pueden superar el formato de compra del papel");
            }
            return new RemnantPolicy(true, minW, minH, remnantUnit);
        }
    }

    public String getPaperId() {
        return paperId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getGrammage() {
        return grammage;
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

    public boolean isCoated() {
        return coated;
    }

    public boolean isAcceptsRemnants() {
        return acceptsRemnants;
    }

    public BigDecimal getMinRemnantWidth() {
        return minRemnantWidth;
    }

    public BigDecimal getMinRemnantHeight() {
        return minRemnantHeight;
    }

    public String getMinRemnantUnit() {
        return minRemnantUnit;
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
        if (!(o instanceof Paper that)) {
            return false;
        }
        return Objects.equals(paperId, that.paperId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paperId);
    }
}
