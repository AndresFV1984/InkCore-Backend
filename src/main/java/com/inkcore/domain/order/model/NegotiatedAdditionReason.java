package com.inkcore.domain.order.model;

import java.util.Arrays;
import java.util.Locale;

public enum NegotiatedAdditionReason {
    SOLICITUD_CLIENTE("solicitud_cliente"),
    CONDICION_NO_PREVISTA("condicion_no_prevista"),
    AJUSTE_PLANTA("ajuste_planta"),
    OTRO("otro");

    private final String dbValue;

    NegotiatedAdditionReason(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static NegotiatedAdditionReason fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("changeReason es obligatorio");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(reason -> reason.dbValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("changeReason inválido: " + value));
    }
}
