package com.inkcore.domain.order.model;

import java.util.Arrays;
import java.util.Locale;

public enum PaymentMethod {
    EFECTIVO("efectivo"),
    TRANSFERENCIA("transferencia"),
    CHEQUE("cheque"),
    TARJETA("tarjeta"),
    OTRO("otro");

    private final String dbValue;

    PaymentMethod(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static PaymentMethod fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("paymentMethod es obligatorio");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(t -> t.dbValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("paymentMethod inválido: " + value));
    }
}
