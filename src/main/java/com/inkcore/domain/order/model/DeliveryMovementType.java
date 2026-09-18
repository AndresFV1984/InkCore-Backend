package com.inkcore.domain.order.model;

import java.util.Arrays;
import java.util.Locale;

public enum DeliveryMovementType {
    ENTREGA("entrega"),
    REVERSION("reversion");

    private final String dbValue;

    DeliveryMovementType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static DeliveryMovementType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return ENTREGA;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(t -> t.dbValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("movementType inválido: " + value));
    }
}
