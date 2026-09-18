package com.inkcore.domain.order.model;

import java.util.Arrays;
import java.util.Locale;

public enum WithholdingType {
    RETEFUENTE("retefuente"),
    RETEIVA("reteiva"),
    RETEICA("reteica"),
    OTRO("otro");

    private final String dbValue;

    WithholdingType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static WithholdingType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(t -> t.dbValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("withholdingType inválido: " + value));
    }
}
