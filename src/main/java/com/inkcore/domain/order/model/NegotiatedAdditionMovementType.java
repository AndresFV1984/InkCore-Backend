package com.inkcore.domain.order.model;

import java.util.Arrays;
import java.util.Locale;

public enum NegotiatedAdditionMovementType {
    REGISTRO("registro"),
    ANULACION("anulacion");

    private final String dbValue;

    NegotiatedAdditionMovementType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public boolean isReversibleRegistration() {
        return this == REGISTRO;
    }

    public static NegotiatedAdditionMovementType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("movementType es obligatorio");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(type -> type.dbValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("movementType inválido: " + value));
    }
}
