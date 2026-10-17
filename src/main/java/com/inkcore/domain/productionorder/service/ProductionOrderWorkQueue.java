package com.inkcore.domain.productionorder.service;

/**
 * Cola de trabajo de la orden completa. La posición la define el administrador
 * y la heredan todas las etapas. Finalizada y anulada salen de la cola.
 */
public final class ProductionOrderWorkQueue {

    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_ANULADA = "ANULADA";

    private ProductionOrderWorkQueue() {
    }

    public static boolean isOpen(String status, boolean state) {
        if (!state || status == null || status.isBlank()) {
            return false;
        }
        return !STATUS_COMPLETED.equals(status) && !STATUS_ANULADA.equals(status);
    }
}
