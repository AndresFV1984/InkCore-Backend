package com.inkcore.application.productionorder.usecase;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload de una etapa tercerizada (PUT paso o billing).
 */
public record OutsourcedStageCommand(
        String stage,
        Boolean enabled,
        BigDecimal vendorCost,
        BigDecimal markupPercent,
        BigDecimal sellPrice,
        String supplierId,
        String supplierName,
        LocalDate quotedAt,
        Integer quantity,
        String costResponsibleUserId,
        String costResponsibleUserName,
        LocalDate quoteValidUntil,
        LocalDate sentAt,
        String sentDescription,
        LocalDate expectedReturnAt,
        LocalDate receivedAt,
        String receivedByUserId,
        String fulfillmentStatus
) {
    public OutsourcedStageCommand withStageIfMissing(String stageName) {
        if (stage != null && !stage.isBlank()) {
            return this;
        }
        return new OutsourcedStageCommand(
                stageName,
                enabled,
                vendorCost,
                markupPercent,
                sellPrice,
                supplierId,
                supplierName,
                quotedAt,
                quantity,
                costResponsibleUserId,
                costResponsibleUserName,
                quoteValidUntil,
                sentAt,
                sentDescription,
                expectedReturnAt,
                receivedAt,
                receivedByUserId,
                fulfillmentStatus
        );
    }
}
