package com.inkcore.domain.thousandrate.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado tarifa por millar. Campos alineados a {@code indicolors.thousand_rates}.
 */
public final class ThousandRate {

    private static final int DEFAULT_THOUSAND_UNIT = 1000;

    private final String thousandRateId;
    private final String companyId;
    private final String name;
    private final String colorCategory;
    private final int thousandUnit;
    private final BigDecimal price;
    private final boolean state;
    private final int minThresholdUnits;
    private final BigDecimal minThousand;
    private final BigDecimal decimalThreshold;
    private final BigDecimal gripperFlipPrice;
    private final BigDecimal squareFlipPrice;
    private final boolean isDefault;
    private final LocalDate creationDate;

    private ThousandRate(
            String thousandRateId,
            String companyId,
            String name,
            String colorCategory,
            int thousandUnit,
            BigDecimal price,
            boolean state,
            int minThresholdUnits,
            BigDecimal minThousand,
            BigDecimal decimalThreshold,
            BigDecimal gripperFlipPrice,
            BigDecimal squareFlipPrice,
            boolean isDefault,
            LocalDate creationDate
    ) {
        this.thousandRateId = thousandRateId;
        this.companyId = companyId;
        this.name = name;
        this.colorCategory = colorCategory;
        this.thousandUnit = thousandUnit;
        this.price = price;
        this.state = state;
        this.minThresholdUnits = minThresholdUnits;
        this.minThousand = minThousand;
        this.decimalThreshold = decimalThreshold;
        this.gripperFlipPrice = gripperFlipPrice;
        this.squareFlipPrice = squareFlipPrice;
        this.isDefault = isDefault;
        this.creationDate = creationDate;
    }

    public static ThousandRate createNew(
            String companyId,
            String name,
            String colorCategory,
            Integer thousandUnit,
            BigDecimal price,
            boolean state,
            Integer minThresholdUnits,
            BigDecimal minThousand,
            BigDecimal decimalThreshold,
            BigDecimal gripperFlipPrice,
            BigDecimal squareFlipPrice,
            boolean isDefault,
            LocalDate creationDate
    ) {
        requireNotBlank(companyId, "La empresa es obligatoria");
        requireNotBlank(name, "El nombre es obligatorio");
        requireNotBlank(colorCategory, "La categoría de color es obligatoria");
        return new ThousandRate(
                UUID.randomUUID().toString(),
                companyId.trim(),
                name.trim(),
                colorCategory.trim(),
                normalizeThousandUnit(thousandUnit),
                normalizeNonNegativeMoney(price, "El precio es obligatorio", "El precio no puede ser negativo"),
                state,
                normalizePositiveInt(minThresholdUnits, "El tope mínimo millar es obligatorio",
                        "El tope mínimo millar debe ser mayor que 0"),
                normalizePositiveMoney(minThousand, "El millar mínimo es obligatorio",
                        "El millar mínimo debe ser mayor que 0"),
                normalizeDecimalThreshold(decimalThreshold),
                normalizeOptionalNonNegativeMoney(gripperFlipPrice, "El precio de volteo por pinza no puede ser negativo"),
                normalizeOptionalNonNegativeMoney(squareFlipPrice, "El precio de volteo por escuadra no puede ser negativo"),
                isDefault,
                creationDate
        );
    }

    public ThousandRate update(
            String name,
            String colorCategory,
            Integer thousandUnit,
            BigDecimal price,
            boolean state,
            Integer minThresholdUnits,
            BigDecimal minThousand,
            BigDecimal decimalThreshold,
            BigDecimal gripperFlipPrice,
            BigDecimal squareFlipPrice,
            boolean isDefault
    ) {
        requireNotBlank(name, "El nombre es obligatorio");
        requireNotBlank(colorCategory, "La categoría de color es obligatoria");
        return new ThousandRate(
                this.thousandRateId,
                this.companyId,
                name.trim(),
                colorCategory.trim(),
                normalizeThousandUnit(thousandUnit),
                normalizeNonNegativeMoney(price, "El precio es obligatorio", "El precio no puede ser negativo"),
                state,
                normalizePositiveInt(minThresholdUnits, "El tope mínimo millar es obligatorio",
                        "El tope mínimo millar debe ser mayor que 0"),
                normalizePositiveMoney(minThousand, "El millar mínimo es obligatorio",
                        "El millar mínimo debe ser mayor que 0"),
                normalizeDecimalThreshold(decimalThreshold),
                normalizeOptionalNonNegativeMoney(gripperFlipPrice, "El precio de volteo por pinza no puede ser negativo"),
                normalizeOptionalNonNegativeMoney(squareFlipPrice, "El precio de volteo por escuadra no puede ser negativo"),
                isDefault,
                this.creationDate
        );
    }

    public static ThousandRate reconstitute(
            String thousandRateId,
            String companyId,
            String name,
            String colorCategory,
            int thousandUnit,
            BigDecimal price,
            boolean state,
            int minThresholdUnits,
            BigDecimal minThousand,
            BigDecimal decimalThreshold,
            BigDecimal gripperFlipPrice,
            BigDecimal squareFlipPrice,
            boolean isDefault,
            LocalDate creationDate
    ) {
        return new ThousandRate(
                thousandRateId,
                companyId,
                name,
                colorCategory,
                thousandUnit,
                price,
                state,
                minThresholdUnits,
                minThousand,
                decimalThreshold,
                gripperFlipPrice,
                squareFlipPrice,
                isDefault,
                creationDate
        );
    }

    private static int normalizeThousandUnit(Integer value) {
        int unit = value == null ? DEFAULT_THOUSAND_UNIT : value;
        if (unit <= 0) {
            throw new IllegalArgumentException("La unidad millar debe ser mayor que 0");
        }
        return unit;
    }

    private static int normalizePositiveInt(Integer value, String requiredMessage, String positiveMessage) {
        if (value == null) {
            throw new IllegalArgumentException(requiredMessage);
        }
        if (value <= 0) {
            throw new IllegalArgumentException(positiveMessage);
        }
        return value;
    }

    private static BigDecimal normalizeNonNegativeMoney(BigDecimal value, String requiredMessage, String negativeMessage) {
        if (value == null) {
            throw new IllegalArgumentException(requiredMessage);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(negativeMessage);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizePositiveMoney(BigDecimal value, String requiredMessage, String positiveMessage) {
        if (value == null) {
            throw new IllegalArgumentException(requiredMessage);
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(positiveMessage);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizeDecimalThreshold(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("El umbral decimal es obligatorio");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("El umbral decimal debe estar entre 0 y 1");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal normalizeOptionalNonNegativeMoney(BigDecimal value, String negativeMessage) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(negativeMessage);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    public String getThousandRateId() {
        return thousandRateId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public String getColorCategory() {
        return colorCategory;
    }

    public int getThousandUnit() {
        return thousandUnit;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isState() {
        return state;
    }

    public int getMinThresholdUnits() {
        return minThresholdUnits;
    }

    public BigDecimal getMinThousand() {
        return minThousand;
    }

    public BigDecimal getDecimalThreshold() {
        return decimalThreshold;
    }

    public BigDecimal getGripperFlipPrice() {
        return gripperFlipPrice;
    }

    public BigDecimal getSquareFlipPrice() {
        return squareFlipPrice;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ThousandRate that)) {
            return false;
        }
        return Objects.equals(thousandRateId, that.thousandRateId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(thousandRateId);
    }
}
