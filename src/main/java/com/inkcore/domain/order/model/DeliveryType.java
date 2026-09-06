package com.inkcore.domain.order.model;

import java.util.Arrays;
import java.util.Locale;

public enum DeliveryType {
    PARCIAL("parcial"),
    TOTAL("total");

    private final String dbValue;

    DeliveryType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static DeliveryType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("deliveryType es obligatorio");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(t -> t.dbValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("deliveryType inválido: " + value));
    }
}
