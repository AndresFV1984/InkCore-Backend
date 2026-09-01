package com.inkcore.infrastructure.in.rest.productionorders;

import com.inkcore.domain.productionorder.model.BillingDetails;
import com.inkcore.domain.productionorder.model.ClientCostingMode;
import com.inkcore.domain.productionorder.model.ClientPlateType;
import com.inkcore.domain.productionorder.model.DiscountType;
import com.inkcore.domain.productionorder.model.FlipType;
import com.inkcore.domain.productionorder.model.OperatorAssignment;
import com.inkcore.domain.productionorder.model.PaperRow;
import com.inkcore.domain.productionorder.model.Plate;
import com.inkcore.domain.productionorder.model.PostpressLine;
import com.inkcore.domain.productionorder.model.PostpressRecord;
import com.inkcore.domain.productionorder.model.PrepressDetails;
import com.inkcore.domain.productionorder.model.PrintConfig;
import com.inkcore.domain.productionorder.model.PrintEntry;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.StageDiscount;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(name = "ProductionOrderResponse", description = "Agregado completo de Orden de Producción")
public record ProductionOrderResponse(
        @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String productionOrderId,
        @Schema(example = "company-seed-001")
        String companyId,
        @Schema(
                example = "OP-42",
                maxLength = 20,
                pattern = "^OP-[0-9]+$",
                description = "Consecutivo corto único por empresa, generado por el backend (OP-1, OP-2, …). "
                        + "No se envía en el alta; el front lo usa para mostrar y buscar."
        )
        String orderNumber,
        @Schema(description = "Versión optimista", example = "0")
        Long version,
        String clientId,
        String workName,
        String sellerId,
        LocalDate orderDate,
        Integer requestedQuantity,
        Integer proposalQuantity1,
        Integer proposalQuantity2,
        LocalDateTime specificationsCompletedAt,
        LocalDateTime cuttingCompletedAt,
        LocalDateTime printingCompletedAt,
        LocalDateTime finishedProductsCompletedAt,
        LocalDateTime finishingProcessesCompletedAt,
        Boolean clientSuppliesPaperDefault,
        Integer roundingMargin,
        String status,
        Boolean state,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String updatedBy,
        PrepressResponse prepress,
        BillingResponse billing,
        List<OperatorResponse> operators,
        List<StageDiscountResponse> stageDiscounts,
        List<PlateResponse> plates,
        List<PaperRowResponse> paperRows,
        List<PrintResponse> prints,
        List<PostpressRecordResponse> postpressRecords
) {
    public static ProductionOrderResponse from(ProductionOrder order) {
        return new ProductionOrderResponse(
                order.getProductionOrderId(),
                order.getCompanyId(),
                order.getOrderNumber(),
                order.getVersion(),
                order.getClientId(),
                order.getWorkName(),
                order.getSellerId(),
                order.getOrderDate(),
                order.getRequestedQuantity(),
                order.getProposalQuantity1(),
                order.getProposalQuantity2(),
                order.getSpecificationsCompletedAt(),
                order.getCuttingCompletedAt(),
                order.getPrintingCompletedAt(),
                order.getFinishedProductsCompletedAt(),
                order.getFinishingProcessesCompletedAt(),
                order.getClientSuppliesPaperDefault(),
                order.getRoundingMargin(),
                order.getStatus(),
                order.isState(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getCreatedBy(),
                order.getUpdatedBy(),
                PrepressResponse.from(order.getPrepress()),
                BillingResponse.from(order.getBilling()),
                order.getOperators().stream().map(OperatorResponse::from).toList(),
                order.getStageDiscounts().stream().map(StageDiscountResponse::from).toList(),
                order.getPlates().stream().map(PlateResponse::from).toList(),
                order.getPaperRows().stream().map(PaperRowResponse::from).toList(),
                order.getPrints().stream().map(PrintResponse::from).toList(),
                order.getPostpressRecords().stream().map(PostpressRecordResponse::from).toList()
        );
    }

    public record PrepressResponse(
            Boolean isNewDesign,
            String designName,
            String existingDesignOrderId,
            Boolean hasDesignCost,
            BigDecimal designCost,
            Boolean clientSuppliesPlates,
            String clientPlateType,
            BigDecimal newPlateCost,
            String assemblyPriceId,
            String assemblyPriceName,
            BigDecimal assemblyPriceCost,
            Boolean dieCutLine,
            Boolean uvReserve,
            Boolean stamping,
            Boolean embossing,
            BigDecimal totalPlatesValue,
            String prepressDiscountType,
            BigDecimal prepressDiscountValue,
            LocalDateTime prepressCompletedAt
    ) {
        static PrepressResponse from(PrepressDetails d) {
            if (d == null) {
                return null;
            }
            return new PrepressResponse(
                    d.getNewDesign(),
                    d.getDesignName(),
                    d.getExistingDesignOrderId(),
                    d.isHasDesignCost(),
                    d.getDesignCost(),
                    d.getClientSuppliesPlates(),
                    ClientPlateType.toValue(d.getClientPlateType()),
                    d.getNewPlateCost(),
                    d.getAssemblyPriceId(),
                    d.getAssemblyPriceName(),
                    d.getAssemblyPriceCost(),
                    d.isDieCutLine(),
                    d.isUvReserve(),
                    d.isStamping(),
                    d.isEmbossing(),
                    d.getTotalPlatesValue(),
                    DiscountType.toValue(d.getPrepressDiscountType()),
                    d.getPrepressDiscountValue(),
                    d.getPrepressCompletedAt()
            );
        }
    }

    public record BillingResponse(
            String billingDiscountType,
            BigDecimal billingDiscountValue,
            String clientCostingMode,
            String clientDiscountType,
            BigDecimal clientDiscountValue,
            String clientProfitabilityType,
            BigDecimal clientProfitabilityValue,
            List<Map<String, Object>> clientVolumeCosting,
            LocalDate deliveryStartDate,
            LocalDate deliveryEndDate,
            BigDecimal advancePercentage,
            String clientSignatureName,
            String bankAccountId,
            LocalDateTime billingCompletedAt
    ) {
        static BillingResponse from(BillingDetails d) {
            if (d == null) {
                return null;
            }
            return new BillingResponse(
                    DiscountType.toValue(d.getBillingDiscountType()),
                    d.getBillingDiscountValue(),
                    ClientCostingMode.toValue(d.getClientCostingMode()),
                    DiscountType.toValue(d.getClientDiscountType()),
                    d.getClientDiscountValue(),
                    DiscountType.toValue(d.getClientProfitabilityType()),
                    d.getClientProfitabilityValue(),
                    d.getClientVolumeCosting(),
                    d.getDeliveryStartDate(),
                    d.getDeliveryEndDate(),
                    d.getAdvancePercentage(),
                    d.getClientSignatureName(),
                    d.getBankAccountId(),
                    d.getBillingCompletedAt()
            );
        }
    }

    public record OperatorResponse(String stage, String userId, String roleCode) {
        static OperatorResponse from(OperatorAssignment o) {
            return new OperatorResponse(o.getStage().name(), o.getUserId(), o.getRoleCode());
        }
    }

    public record StageDiscountResponse(String stage, String discountType, BigDecimal discountValue) {
        static StageDiscountResponse from(StageDiscount d) {
            return new StageDiscountResponse(
                    d.getStage().name(),
                    DiscountType.toValue(d.getDiscountType()),
                    d.getDiscountValue()
            );
        }
    }

    public record PlateResponse(
            @Schema(description = "ID de plancha en servidor (usar como plateId en corte/impresión/postprensa)")
            String productionOrderPlateId,
            String colors,
            String plateTypeId,
            String plateName,
            String plateSize,
            BigDecimal platePrice,
            Integer quantity,
            Integer cavities,
            Integer goodSizes,
            Integer surplus,
            Integer platesCount,
            BigDecimal totalValue,
            String detail,
            String observation,
            Boolean manualEntry,
            String plateSupply,
            Boolean plateReplacement,
            Integer replacementQuantity
    ) {
        static PlateResponse from(Plate p) {
            return new PlateResponse(
                    p.getPlateId(),
                    p.getColors(),
                    p.getPlateTypeId(),
                    p.getPlateName(),
                    p.getPlateSize(),
                    p.getPlatePrice(),
                    p.getQuantity(),
                    p.getCavities(),
                    p.getGoodSizes(),
                    p.getSurplus(),
                    p.getPlatesCount(),
                    p.getTotalValue(),
                    p.getDetail(),
                    p.getObservation(),
                    p.isManualEntry(),
                    p.getPlateSupply(),
                    p.isPlateReplacement(),
                    p.getReplacementQuantity()
            );
        }
    }

    public record PaperRowResponse(
            String productionOrderPaperRowId,
            @Schema(description = "Debe coincidir con plates[].productionOrderPlateId")
            String plateId,
            String parentRowId,
            String cutRowKey,
            Boolean isMissingSupply,
            Integer missingSheetsQuantity,
            Boolean clientSuppliesPaper,
            String paperTypeId,
            String supplierId,
            String paperName,
            String paperSize,
            BigDecimal sheetValue,
            Integer packageUnit,
            Boolean isCoated,
            String cutLayoutId,
            String cutLayoutName,
            String cutLayoutSize,
            Integer piecesPerSheet,
            BigDecimal cutValue,
            Boolean isPaperCut,
            Integer deliveredSheetsByClient,
            Integer manualGoodSizes,
            Integer manualSurplus,
            Integer calculatedSheetsCount,
            BigDecimal totalPaperValue,
            BigDecimal totalCutValue
    ) {
        static PaperRowResponse from(PaperRow r) {
            return new PaperRowResponse(
                    r.getPaperRowId(),
                    r.getPlateId(),
                    r.getParentRowId(),
                    r.getCutRowKey(),
                    r.isMissingSupply(),
                    r.getMissingSheetsQuantity(),
                    r.isClientSuppliesPaper(),
                    r.getPaperTypeId(),
                    r.getSupplierId(),
                    r.getPaperName(),
                    r.getPaperSize(),
                    r.getSheetValue(),
                    r.getPackageUnit(),
                    r.getCoated(),
                    r.getCutLayoutId(),
                    r.getCutLayoutName(),
                    r.getCutLayoutSize(),
                    r.getPiecesPerSheet(),
                    r.getCutValue(),
                    r.getPaperCut(),
                    r.getDeliveredSheetsByClient(),
                    r.getManualGoodSizes(),
                    r.getManualSurplus(),
                    r.getCalculatedSheetsCount(),
                    r.getTotalPaperValue(),
                    r.getTotalCutValue()
            );
        }
    }

    public record PrintResponse(
            String productionOrderPrintId,
            @Schema(description = "Debe coincidir con plates[].productionOrderPlateId")
            String plateId,
            Boolean clientSuppliesSherpa,
            BigDecimal sherpaTestPrice,
            BigDecimal machineOutputValue,
            Map<String, Object> inkEstimation,
            String printingDiscountType,
            BigDecimal printingDiscountValue,
            Boolean completed,
            List<PrintEntryResponse> entries
    ) {
        static PrintResponse from(PrintConfig p) {
            return new PrintResponse(
                    p.getPrintId(),
                    p.getPlateId(),
                    p.getClientSuppliesSherpa(),
                    p.getSherpaTestPrice(),
                    p.getMachineOutputValue(),
                    p.getInkEstimation(),
                    DiscountType.toValue(p.getPrintingDiscountType()),
                    p.getPrintingDiscountValue(),
                    p.isCompleted(),
                    p.getEntries().stream().map(PrintEntryResponse::from).toList()
            );
        }
    }

    public record PrintEntryResponse(
            String productionOrderPrintEntryId,
            Integer shotsInkCount,
            List<String> shotsInks,
            Integer reverseInkCount,
            List<String> reverseInks,
            String basicFlipType,
            String basicThousandRateId,
            String basicRateName,
            BigDecimal basicRatePrice,
            BigDecimal basicRateGripperFlipPrice,
            BigDecimal basicRateSquareFlipPrice,
            BigDecimal basicCalculatedThousands,
            BigDecimal basicPrintingPrice,
            String pantoneFlipType,
            Boolean clientSuppliesPantoneInk,
            BigDecimal pantoneInkChargePrice,
            String pantoneThousandRateId,
            String pantoneRateName,
            BigDecimal pantoneRatePrice,
            BigDecimal pantoneRateGripperFlipPrice,
            BigDecimal pantoneRateSquareFlipPrice,
            BigDecimal pantoneCalculatedThousands,
            BigDecimal pantonePrintingPrice
    ) {
        static PrintEntryResponse from(PrintEntry e) {
            return new PrintEntryResponse(
                    e.getPrintEntryId(),
                    e.getShotsInkCount(),
                    e.getShotsInks(),
                    e.getReverseInkCount(),
                    e.getReverseInks(),
                    FlipType.toValue(e.getBasicFlipType()),
                    e.getBasicThousandRateId(),
                    e.getBasicRateName(),
                    e.getBasicRatePrice(),
                    e.getBasicRateGripperFlipPrice(),
                    e.getBasicRateSquareFlipPrice(),
                    e.getBasicCalculatedThousands(),
                    e.getBasicPrintingPrice(),
                    FlipType.toValue(e.getPantoneFlipType()),
                    e.getClientSuppliesPantoneInk(),
                    e.getPantoneInkChargePrice(),
                    e.getPantoneThousandRateId(),
                    e.getPantoneRateName(),
                    e.getPantoneRatePrice(),
                    e.getPantoneRateGripperFlipPrice(),
                    e.getPantoneRateSquareFlipPrice(),
                    e.getPantoneCalculatedThousands(),
                    e.getPantonePrintingPrice()
            );
        }
    }

    public record PostpressRecordResponse(
            String productionOrderPostpressRecordId,
            @Schema(description = "Debe coincidir con plates[].productionOrderPlateId")
            String plateId,
            @Schema(allowableValues = {"FINISHED_PRODUCT", "FINISHING_PROCESS"})
            String type,
            Boolean completed,
            List<PostpressLineResponse> lines
    ) {
        static PostpressRecordResponse from(PostpressRecord r) {
            return new PostpressRecordResponse(
                    r.getRecordId(),
                    r.getPlateId(),
                    r.getType() == null ? null : r.getType().name(),
                    r.isCompleted(),
                    r.getLines().stream().map(PostpressLineResponse::from).toList()
            );
        }
    }

    public record PostpressLineResponse(
            String productionOrderPostpressLineId,
            String catalogItemId,
            String itemName,
            String source,
            BigDecimal valuePerCm2,
            BigDecimal minCost,
            BigDecimal areaFactor,
            Integer goodSizes,
            BigDecimal calculatedPrice,
            BigDecimal chargedPrice,
            Boolean appliedMinCost,
            Boolean positive,
            Boolean cliche
    ) {
        static PostpressLineResponse from(PostpressLine l) {
            return new PostpressLineResponse(
                    l.getLineId(),
                    l.getCatalogItemId(),
                    l.getItemName(),
                    l.getSource(),
                    l.getValuePerCm2(),
                    l.getMinCost(),
                    l.getAreaFactor(),
                    l.getGoodSizes(),
                    l.getCalculatedPrice(),
                    l.getChargedPrice(),
                    l.isAppliedMinCost(),
                    l.getPositive(),
                    l.getCliche()
            );
        }
    }
}
