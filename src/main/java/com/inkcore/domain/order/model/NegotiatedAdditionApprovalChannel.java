package com.inkcore.domain.order.model;

import java.util.Arrays;
import java.util.Locale;

public enum NegotiatedAdditionApprovalChannel {
    CORREO("correo"),
    WHATSAPP("whatsapp"),
    FIRMA("firma"),
    OTRO("otro");

    private final String dbValue;

    NegotiatedAdditionApprovalChannel(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static NegotiatedAdditionApprovalChannel fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("approvalChannel es obligatorio");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(channel -> channel.dbValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("approvalChannel inválido: " + value));
    }
}
