package com.inkcore.domain.paper.model;

/**
 * Regla de selección de proveedor al cotizar papel.
 */
public enum PriceRule {
    PREFERRED,
    REPLACEMENT,
    BEST_COST;

    public static PriceRule fromApiValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La regla de precio es obligatoria");
        }
        try {
            return PriceRule.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "La regla de precio debe ser PREFERRED, REPLACEMENT o BEST_COST");
        }
    }

    public String toApiValue() {
        return name();
    }
}
