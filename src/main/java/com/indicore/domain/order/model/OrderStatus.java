package com.indicore.domain.order.model;

public enum OrderStatus {
    EN_CURSO("En curso"),
    REVISION("Revisión"),
    LISTO("Listo"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
