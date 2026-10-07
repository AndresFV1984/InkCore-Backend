package com.inkcore.domain.paper.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Snapshot de auditoría de precio de papel (solo lectura vía API).
 */
public record PaperPriceHistory(
        String paperPriceHistoryId,
        String companyId,
        String paperId,
        String supplierId,
        BigDecimal sheetValue,
        int packageUnit,
        BigDecimal freightPerSheet,
        Integer minPurchaseSheets,
        Integer paymentDays,
        Integer deliveryDays,
        LocalDate priceDate,
        boolean preferred,
        boolean state,
        LocalDateTime effectiveFrom,
        String changedBy
) {
}
