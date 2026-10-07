package com.inkcore.infrastructure.in.rest.papers;

import com.inkcore.domain.paper.model.PaperCutLayout;
import com.inkcore.domain.paper.model.PaperPriceHistory;
import com.inkcore.domain.paper.model.PaperRemnant;
import com.inkcore.domain.paper.model.PaperStock;
import com.inkcore.domain.paper.model.PaperSupplierPrice;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class PaperNestedResponses {

    private PaperNestedResponses() {
    }

    @Schema(name = "PaperPriceResponse", description = "Precio vigente por proveedor. landedCostPerSheet es calculado (solo lectura).")
    public record PaperPriceResponse(
            @Schema(description = "Identificador del precio", example = "paper-price-seed-001")
            String paperSupplierPriceId,
            @Schema(description = "Proveedor", example = "supplier-seed-001")
            String supplierId,
            @Schema(description = "Precio por pliego", example = "1500.00")
            BigDecimal sheetValue,
            @Schema(description = "Unidad de empaque (pliegos por empaque del proveedor)", example = "500")
            int packageUnit,
            @Schema(description = "Flete por pliego", example = "0.00")
            BigDecimal freightPerSheet,
            @Schema(description = "Mínimo de pliegos de compra", example = "500")
            Integer minPurchaseSheets,
            @Schema(description = "Plazo de pago en días", example = "30")
            Integer paymentDays,
            @Schema(description = "Días de entrega estimados", example = "5")
            Integer deliveryDays,
            @Schema(description = "Fecha de vigencia", example = "2026-10-06")
            LocalDate priceDate,
            @Schema(description = "Proveedor preferido", example = "true")
            boolean preferred,
            @Schema(description = "Activo", example = "true")
            boolean state,
            @Schema(description = "sheetValue + freightPerSheet (generada en BD)", example = "1500.00")
            BigDecimal landedCostPerSheet
    ) {
        public static PaperPriceResponse from(PaperSupplierPrice price) {
            return new PaperPriceResponse(
                    price.getPaperSupplierPriceId(),
                    price.getSupplierId(),
                    price.getSheetValue(),
                    price.getPackageUnit(),
                    price.getFreightPerSheet(),
                    price.getMinPurchaseSheets(),
                    price.getPaymentDays(),
                    price.getDeliveryDays(),
                    price.getPriceDate(),
                    price.isPreferred(),
                    price.isState(),
                    price.getLandedCostPerSheet()
            );
        }
    }

    @Schema(name = "PaperPriceHistoryResponse", description = "Snapshot histórico de precio (append-only; solo lectura)")
    public record PaperPriceHistoryResponse(
            @Schema(example = "paper-price-hist-001")
            String paperPriceHistoryId,
            @Schema(example = "supplier-seed-001")
            String supplierId,
            @Schema(example = "1500.00")
            BigDecimal sheetValue,
            @Schema(description = "Unidad de empaque del snapshot", example = "500")
            int packageUnit,
            @Schema(example = "0.00")
            BigDecimal freightPerSheet,
            Integer minPurchaseSheets,
            Integer paymentDays,
            Integer deliveryDays,
            LocalDate priceDate,
            boolean preferred,
            boolean state,
            @Schema(description = "Desde cuándo aplica este snapshot", example = "2026-10-06T12:00:00")
            LocalDateTime effectiveFrom,
            @Schema(description = "Usuario que generó el cambio; null si fue automático")
            String changedBy
    ) {
        public static PaperPriceHistoryResponse from(PaperPriceHistory history) {
            return new PaperPriceHistoryResponse(
                    history.paperPriceHistoryId(),
                    history.supplierId(),
                    history.sheetValue(),
                    history.packageUnit(),
                    history.freightPerSheet(),
                    history.minPurchaseSheets(),
                    history.paymentDays(),
                    history.deliveryDays(),
                    history.priceDate(),
                    history.preferred(),
                    history.state(),
                    history.effectiveFrom(),
                    history.changedBy()
            );
        }
    }

    @Schema(name = "PaperCutLayoutResponse",
            description = "Despiece asociado a un papel. wastePercentage es sugerencia de UI, no merma de cálculo OP.")
    public record PaperCutLayoutResponse(
            @Schema(example = "paper-cut-seed-001")
            String paperCutLayoutId,
            @Schema(example = "paper-seed-001")
            String paperId,
            @Schema(example = "cut-layout-seed-001")
            String cutLayoutId,
            @Schema(description = "vertical|horizontal", example = "vertical", allowableValues = {"vertical", "horizontal"})
            String orientation,
            @Schema(description = "Sugerencia UI de desperdicio %; no sustituye company_waste_settings", example = "2.00")
            BigDecimal wastePercentage,
            String note,
            @Schema(example = "true")
            boolean state
    ) {
        public static PaperCutLayoutResponse from(PaperCutLayout layout) {
            return new PaperCutLayoutResponse(
                    layout.getPaperCutLayoutId(),
                    layout.getPaperId(),
                    layout.getCutLayoutId(),
                    layout.getOrientation(),
                    layout.getWastePercentage(),
                    layout.getNote(),
                    layout.isState()
            );
        }
    }

    @Schema(name = "PaperStockResponse", description = "Lote de inventario de papel (sin consumo automático aún)")
    public record PaperStockResponse(
            @Schema(example = "paper-stock-seed-001")
            String paperStockId,
            @Schema(example = "paper-seed-001")
            String paperId,
            @Schema(example = "1000.00")
            BigDecimal quantityInitial,
            @Schema(example = "850.00")
            BigDecimal quantityAvailable,
            @Schema(example = "1500.00")
            BigDecimal unitCost,
            @Schema(example = "2026-10-06")
            LocalDate entryDate,
            @Schema(example = "true")
            boolean state
    ) {
        public static PaperStockResponse from(PaperStock stock) {
            return new PaperStockResponse(
                    stock.getPaperStockId(),
                    stock.getPaperId(),
                    stock.getQuantityInitial(),
                    stock.getQuantityAvailable(),
                    stock.getUnitCost(),
                    stock.getEntryDate(),
                    stock.isState()
            );
        }
    }

    @Schema(name = "PaperRemnantResponse",
            description = "Remanente reutilizable de corte: material del papel con medidas propias.")
    public record PaperRemnantResponse(
            @Schema(example = "paper-remnant-seed-001")
            String paperRemnantId,
            @Schema(example = "paper-seed-001")
            String paperId,
            @Schema(example = "35.00")
            BigDecimal width,
            @Schema(example = "50.00")
            BigDecimal height,
            @Schema(example = "cm", allowableValues = {"cm", "mm", "in"})
            String unit,
            @Schema(example = "12.00")
            BigDecimal quantityInitial,
            @Schema(example = "12.00")
            BigDecimal quantityAvailable,
            @Schema(example = "0.00")
            BigDecimal unitCost,
            @Schema(example = "production-order-seed-001")
            String sourceProductionOrderId,
            @Schema(example = "paper-row-seed-001")
            String sourcePaperRowId,
            @Schema(example = "2026-10-06")
            LocalDate entryDate,
            String note,
            @Schema(example = "true")
            boolean state
    ) {
        public static PaperRemnantResponse from(PaperRemnant remnant) {
            return new PaperRemnantResponse(
                    remnant.getPaperRemnantId(),
                    remnant.getPaperId(),
                    remnant.getWidth(),
                    remnant.getHeight(),
                    remnant.getUnit(),
                    remnant.getQuantityInitial(),
                    remnant.getQuantityAvailable(),
                    remnant.getUnitCost(),
                    remnant.getSourceProductionOrderId(),
                    remnant.getSourcePaperRowId(),
                    remnant.getEntryDate(),
                    remnant.getNote(),
                    remnant.isState()
            );
        }
    }
}
