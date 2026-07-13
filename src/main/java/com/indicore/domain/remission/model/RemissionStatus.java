package com.indicore.domain.remission.model;

public enum RemissionStatus {
    PENDIENTE("Pendiente"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado");

    private final String label;

    RemissionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
