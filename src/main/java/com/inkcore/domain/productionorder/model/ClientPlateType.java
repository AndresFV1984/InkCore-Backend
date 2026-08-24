package com.inkcore.domain.productionorder.model;

/**
 * Origen de la plancha. Coincide con el CHECK de
 * {@code production_order_prepress_details.client_plate_type}.
 */
public enum ClientPlateType {

    CLIENT_SUPPLIES("client-supplies"),
    EXISTING_PLATE("existing-plate"),
    NEW_PLATE("new-plate");

    private final String value;

    ClientPlateType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ClientPlateType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        for (ClientPlateType type : values()) {
            if (type.value.equalsIgnoreCase(trimmed) || type.name().equalsIgnoreCase(trimmed)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de plancha inválido: " + value);
    }

    public static String toValue(ClientPlateType type) {
        return type == null ? null : type.value;
    }
}
