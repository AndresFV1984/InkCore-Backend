package com.inkcore.domain.productionorder.model;

/**
 * Tipo de volteo de impresión. Coincide con el CHECK de
 * {@code production_order_print_entries.basic_flip_type}.
 */
public enum FlipType {

    NO_FLIP("no-flip"),
    GRIPPER_FLIP("gripper-flip"),
    SQUARE_FLIP("square-flip");

    private final String value;

    FlipType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static FlipType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return NO_FLIP;
        }
        String trimmed = value.trim();
        for (FlipType type : values()) {
            if (type.value.equalsIgnoreCase(trimmed) || type.name().equalsIgnoreCase(trimmed)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de volteo inválido: " + value);
    }

    public static String toValue(FlipType type) {
        return type == null ? NO_FLIP.value : type.value;
    }
}
