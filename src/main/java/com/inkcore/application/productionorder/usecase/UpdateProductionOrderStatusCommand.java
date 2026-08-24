package com.inkcore.application.productionorder.usecase;

public record UpdateProductionOrderStatusCommand(
        Long version,
        String status,
        Boolean state
) {
}
