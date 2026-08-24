package com.inkcore.domain.productionorder.model;

/**
 * Tipo de descuento almacenado como {@code %} o {@code $}.
 */
public enum DiscountType {

    PERCENT("%"),
    AMOUNT("$");

    private final String value;

    DiscountType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DiscountType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        for (DiscountType type : values()) {
            if (type.value.equals(trimmed) || type.name().equalsIgnoreCase(trimmed)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de descuento inválido: " + value);
    }

    public static String toValue(DiscountType type) {
        return type == null ? null : type.value;
    }
}
