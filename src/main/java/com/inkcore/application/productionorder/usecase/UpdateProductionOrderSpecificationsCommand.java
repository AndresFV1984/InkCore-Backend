package com.inkcore.application.productionorder.usecase;

import java.time.LocalDate;

public record UpdateProductionOrderSpecificationsCommand(
        Long version,
        String clientId,
        String workName,
        String sellerId,
        LocalDate orderDate,
        Integer requestedQuantity,
        Integer proposalQuantity1,
        Integer proposalQuantity2,
        String operatorUserId
) {
}
