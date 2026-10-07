package com.inkcore.application.paper.usecase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PaperCommands {

    private PaperCommands() {
    }

    public record ReplacePaperPricesCommand(List<ReplacePaperPriceItem> prices) {
    }

    public record ReplacePaperPriceItem(
            String supplierId,
            BigDecimal sheetValue,
            Integer packageUnit,
            BigDecimal freightPerSheet,
            Integer minPurchaseSheets,
            Integer paymentDays,
            Integer deliveryDays,
            LocalDate priceDate,
            Boolean preferred,
            Boolean state
    ) {
    }

    public record CreatePaperCutLayoutCommand(
            String cutLayoutId,
            String orientation,
            BigDecimal wastePercentage,
            String note,
            Boolean state
    ) {
    }

    public record UpdatePaperCutLayoutCommand(
            String orientation,
            BigDecimal wastePercentage,
            String note,
            Boolean state
    ) {
    }

    public record CreatePaperStockCommand(
            BigDecimal quantityInitial,
            BigDecimal quantityAvailable,
            BigDecimal unitCost,
            LocalDate entryDate,
            Boolean state
    ) {
    }

    public record UpdatePaperStockCommand(
            BigDecimal quantityInitial,
            BigDecimal quantityAvailable,
            BigDecimal unitCost,
            LocalDate entryDate,
            Boolean state
    ) {
    }

    public record CreatePaperRemnantCommand(
            BigDecimal width,
            BigDecimal height,
            String unit,
            BigDecimal quantityInitial,
            BigDecimal quantityAvailable,
            BigDecimal unitCost,
            String sourceProductionOrderId,
            String sourcePaperRowId,
            LocalDate entryDate,
            String note,
            Boolean state
    ) {
    }

    public record UpdatePaperRemnantCommand(
            BigDecimal width,
            BigDecimal height,
            String unit,
            BigDecimal quantityInitial,
            BigDecimal quantityAvailable,
            BigDecimal unitCost,
            String sourceProductionOrderId,
            String sourcePaperRowId,
            LocalDate entryDate,
            String note,
            Boolean state
    ) {
    }
}
