package com.inkcore.domain.order.model;

import java.util.Arrays;
import java.util.Locale;

public enum PaymentType {
    ABONO("abono"),
    ANTICIPO("anticipo"),
    RETENCION("retencion"),
    REVERSION("reversion");

    private final String dbValue;

    PaymentType(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public boolean isReversibleSettlement() {
        return this == ABONO || this == ANTICIPO || this == RETENCION;
    }

    public static PaymentType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("paymentType es obligatorio");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(t -> t.dbValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("paymentType inválido: " + value));
    }
}
