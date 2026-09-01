package com.inkcore.application.productionorder.usecase;

import java.math.BigDecimal;
import java.util.List;

public record UpdateProductionOrderPostpressCommand(
        Long version,
        Boolean completed,
        List<OperatorAssignmentCommand> operators,
        String operatorUserId,
        String discountType,
        BigDecimal discountValue,
        List<PostpressRecordInput> records
) {
    public record PostpressRecordInput(
            String recordId,
            String plateId,
            Boolean completed,
            List<PostpressLineInput> lines
    ) {
    }

    public record PostpressLineInput(
            String lineId,
            String catalogItemId,
            String source,
            BigDecimal areaFactor,
            Integer goodSizes,
            Boolean positive,
            Boolean cliche
    ) {
    }
}
