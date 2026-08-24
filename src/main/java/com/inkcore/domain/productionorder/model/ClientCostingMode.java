package com.inkcore.domain.productionorder.model;

/**
 * Modo de costeo al cliente. Coincide con el CHECK de
 * {@code production_order_billing_details.client_costing_mode}.
 */
public enum ClientCostingMode {

    EXACT("exact"),
    VOLUME("volume");

    private final String value;

    ClientCostingMode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ClientCostingMode fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        for (ClientCostingMode mode : values()) {
            if (mode.value.equalsIgnoreCase(trimmed) || mode.name().equalsIgnoreCase(trimmed)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Modo de costeo inválido: " + value);
    }

    public static String toValue(ClientCostingMode mode) {
        return mode == null ? null : mode.value;
    }
}
