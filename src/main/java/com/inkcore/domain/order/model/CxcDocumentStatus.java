package com.inkcore.domain.order.model;

import com.inkcore.domain.order.exception.OrderBusinessRuleException;

public enum CxcDocumentStatus {
    PENDIENTE("pendiente"),
    PARCIAL("parcial"),
    PAGADA("pagada"),
    ANULADA("anulada");

    private final String dbValue;

    CxcDocumentStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static CxcDocumentStatus fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return PENDIENTE;
        }
        String normalized = raw.trim().toLowerCase();
        for (CxcDocumentStatus value : values()) {
            if (value.dbValue.equals(normalized)) {
                return value;
            }
        }
        throw new OrderBusinessRuleException("status CxC inválido: " + raw);
    }
}
