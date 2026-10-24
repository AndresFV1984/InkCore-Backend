package com.inkcore.domain.order.model;

import com.inkcore.domain.order.exception.OrderBusinessRuleException;

public enum CxcDocumentConcept {
    ENTREGA("entrega"),
    EXTRA("extra");

    private final String dbValue;

    CxcDocumentConcept(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static CxcDocumentConcept fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return ENTREGA;
        }
        String normalized = raw.trim().toLowerCase();
        for (CxcDocumentConcept value : values()) {
            if (value.dbValue.equals(normalized)) {
                return value;
            }
        }
        throw new OrderBusinessRuleException("paymentConcept/concepto CxC inválido: " + raw);
    }
}
