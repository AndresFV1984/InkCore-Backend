package com.inkcore.domain.productionorder.model;

/**
 * Discriminador de {@code production_order_postpress_records.type}.
 */
public enum PostpressType {

    FINISHED_PRODUCT(ProductionOrderStage.FINISHED_PRODUCTS),
    FINISHING_PROCESS(ProductionOrderStage.FINISHING_PROCESSES);

    private final ProductionOrderStage stage;

    PostpressType(ProductionOrderStage stage) {
        this.stage = stage;
    }

    public ProductionOrderStage getStage() {
        return stage;
    }

    public static PostpressType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (PostpressType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de postprensa inválido: " + value);
    }
}
