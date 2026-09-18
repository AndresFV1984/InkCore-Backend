package com.inkcore.domain.order.model;

import java.util.Arrays;
import java.util.Locale;

public enum AccountsReceivableStatus {
    PENDIENTE("pendiente"),
    PARCIAL("parcial"),
    PAGADO("pagado"),
    ANULADO("anulado");

    private final String dbValue;

    AccountsReceivableStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static AccountsReceivableStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("status es obligatorio");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(t -> t.dbValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("status inválido: " + value));
    }
}
