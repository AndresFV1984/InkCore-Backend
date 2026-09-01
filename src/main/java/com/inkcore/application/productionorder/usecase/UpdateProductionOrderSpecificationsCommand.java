package com.inkcore.application.productionorder.usecase;

import java.time.LocalDate;
import java.util.List;

public record UpdateProductionOrderSpecificationsCommand(
        Long version,
        String clientId,
        String workName,
        String sellerId,
        LocalDate orderDate,
        Integer requestedQuantity,
        Integer proposalQuantity1,
        Integer proposalQuantity2,
        List<OperatorAssignmentCommand> operators,
        String operatorUserId
) {
}
