package com.inkcore.domain.order.model;

import com.inkcore.domain.order.exception.OrderBusinessRuleException;

public enum PaymentApplicationStatus {
    ACTIVO("activo"),
    REVERSADO("reversado");

    private final String dbValue;

    PaymentApplicationStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static PaymentApplicationStatus fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return ACTIVO;
        }
        String normalized = raw.trim().toLowerCase();
        for (PaymentApplicationStatus value : values()) {
            if (value.dbValue.equals(normalized)) {
                return value;
            }
        }
        throw new OrderBusinessRuleException("status aplicación inválido: " + raw);
    }
}
