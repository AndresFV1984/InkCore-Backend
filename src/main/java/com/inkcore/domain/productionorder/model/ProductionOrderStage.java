package com.inkcore.domain.productionorder.model;

/**
 * Etapas del wizard con operador/descuento propio.
 * Coincide con el CHECK de {@code production_order_operators.stage}.
 */
public enum ProductionOrderStage {

    PREPRESS,
    CUTTING,
    PRINTING,
    FINISHED_PRODUCTS,
    FINISHING_PROCESSES,
    BILLING;

    public static ProductionOrderStage fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (ProductionOrderStage stage : values()) {
            if (stage.name().equalsIgnoreCase(value.trim())) {
                return stage;
            }
        }
        throw new IllegalArgumentException("Etapa inválida: " + value);
    }
}
