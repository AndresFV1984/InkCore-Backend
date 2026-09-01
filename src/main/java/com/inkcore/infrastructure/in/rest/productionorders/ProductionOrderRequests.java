package com.inkcore.infrastructure.in.rest.productionorders;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class ProductionOrderRequests {

    private ProductionOrderRequests() {
    }

    @Schema(
            name = "ProductionOrderOperatorRequest",
            description = "Responsable de una etapa. Máximo 1 por stage en la OP."
    )
    public record OperatorRequest(
            @Schema(
                    description = "Etapa del wizard",
                    example = "PREPRESS",
                    allowableValues = {
                            "PREPRESS", "CUTTING", "PRINTING",
                            "FINISHED_PRODUCTS", "FINISHING_PROCESSES", "BILLING"
                    },
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            @NotBlank String stage,
            @Schema(description = "Usuario responsable (misma compañía que la OP)",
                    example = "11111111-1111-1111-1111-111111111111",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String userId,
            @Schema(description = "Código de rol opcional (informativo)", example = "OPERARIO")
            String roleCode
    ) {
    }

    @Schema(
            name = "RegisterProductionOrderRequest",
            description = "Especificaciones iniciales. No incluir orderNumber: lo asigna el backend."
    )
    public record RegisterRequest(
            @Schema(description = "Cliente de la OP", example = "client-seed-001", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String clientId,
            @Schema(description = "Nombre del trabajo", example = "Brochure corporativo", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank String workName,
            @Schema(description = "Vendedor opcional", example = "seller-seed-001")
            String sellerId,
            @Schema(description = "Fecha de la orden (default: hoy)", example = "2026-08-15")
            LocalDate orderDate,
            @Schema(description = "Cantidad solicitada (>0)", example = "1000", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @Positive Integer requestedQuantity,
            @Schema(description = "Cantidad propuesta 1", example = "1500")
            Integer proposalQuantity1,
            @Schema(description = "Cantidad propuesta 2", example = "2000")
            Integer proposalQuantity2,
            @Schema(description = "Operador opcional de la etapa PREPRESS (legacy)", example = "user-seed-001")
            String operatorUserId
    ) {
    }

    @Schema(
            name = "UpdateProductionOrderSpecificationsRequest",
            description = "Si viene la clave operators: replace-all del set de responsables. "
                    + "Si se omite: no se modifican. operators:[] limpia todos."
    )
    public record UpdateSpecificationsRequest(
            @Schema(description = "Versión optimista actual", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Long version,
            @NotBlank String clientId,
            @NotBlank String workName,
            String sellerId,
            LocalDate orderDate,
            @NotNull @Positive Integer requestedQuantity,
            Integer proposalQuantity1,
            Integer proposalQuantity2,
            @Schema(description = "Set completo de responsables. Omitir = no tocar; [] = clear.")
            @Valid List<OperatorRequest> operators,
            @Schema(description = "Legacy: upsert PREPRESS solo si operators está ausente")
            String operatorUserId
    ) {
    }

    @Schema(name = "UpdateProductionOrderStatusRequest")
    public record UpdateStatusRequest(
            @Schema(description = "Versión optimista actual", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Long version,
            @Schema(description = "Estado en planta", example = "IN_PROGRESS",
                    allowableValues = {"PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED"})
            String status,
            @Schema(description = "false = baja lógica (archivar)", example = "false")
            Boolean state
    ) {
    }

    @Schema(name = "UpdateProductionOrderPrepressRequest")
    public record UpdatePrepressRequest(
            @NotNull Long version,
            @Schema(description = "true=diseño nuevo; false=diseño existente", example = "true")
            Boolean isNewDesign,
            @Schema(example = "Diseño brochure 2026")
            String designName,
            @Schema(description = "Obligatorio si isNewDesign=false (self-FK a otra OP)")
            String existingDesignOrderId,
            Boolean hasDesignCost,
            BigDecimal designCost,
            Boolean clientSuppliesPlates,
            @Schema(allowableValues = {"client-supplies", "existing-plate", "new-plate"})
            String clientPlateType,
            BigDecimal newPlateCost,
            String assemblyPriceId,
            Boolean dieCutLine,
            Boolean uvReserve,
            Boolean stamping,
            Boolean embossing,
            @Schema(allowableValues = {"%", "$"})
            String prepressDiscountType,
            BigDecimal prepressDiscountValue,
            Boolean completed,
            @Valid List<OperatorRequest> operators,
            String operatorUserId,
            List<PlateRequest> plates
    ) {
    }

    @Schema(name = "ProductionOrderPlateRequest")
    public record PlateRequest(
            @JsonAlias("productionOrderPlateId")
            String plateId,
            @Schema(example = "4 COLORES")
            String colors,
            String plateTypeId,
            @Schema(example = "1000")
            Integer quantity,
            @Schema(example = "4")
            Integer cavities,
            Integer surplus,
            Integer platesCount,
            String detail,
            String observation,
            Boolean manualEntry,
            String plateSupply,
            Boolean plateReplacement,
            Integer replacementQuantity,
            String plateName,
            String plateSize,
            BigDecimal platePrice
    ) {
    }

    @Schema(name = "UpdateProductionOrderPaperCuttingRequest")
    public record UpdatePaperCuttingRequest(
            @NotNull Long version,
            Boolean clientSuppliesPaperDefault,
            @Schema(example = "2")
            Integer roundingMargin,
            Boolean completed,
            @Valid List<OperatorRequest> operators,
            String operatorUserId,
            @Schema(allowableValues = {"%", "$"})
            String discountType,
            BigDecimal discountValue,
            List<PaperRowRequest> paperRows
    ) {
    }

    @Schema(name = "ProductionOrderPaperRowRequest")
    public record PaperRowRequest(
            @JsonAlias("productionOrderPaperRowId")
            String paperRowId,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                    description = "plates[].productionOrderPlateId del GET")
            String plateId,
            String parentRowId,
            @Schema(example = "cut-1", requiredMode = Schema.RequiredMode.REQUIRED)
            String cutRowKey,
            Boolean isMissingSupply,
            Integer missingSheetsQuantity,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Boolean clientSuppliesPaper,
            String paperTypeId,
            String supplierId,
            String cutLayoutId,
            Boolean isPaperCut,
            Integer deliveredSheetsByClient,
            Integer manualGoodSizes,
            Integer manualSurplus
    ) {
    }

    @Schema(name = "UpdateProductionOrderPrintingRequest")
    public record UpdatePrintingRequest(
            @NotNull Long version,
            Boolean completed,
            @Valid List<OperatorRequest> operators,
            String operatorUserId,
            List<PrintRequest> prints
    ) {
    }

    @Schema(name = "ProductionOrderPrintRequest")
    public record PrintRequest(
            @JsonAlias("productionOrderPrintId")
            String printId,
            String plateId,
            Boolean clientSuppliesSherpa,
            BigDecimal sherpaTestPrice,
            BigDecimal machineOutputValue,
            @Schema(description = "Metadatos de estimación + objectKey/previewObjectKey por entry (sin Base64)")
            Map<String, Object> inkEstimation,
            @Schema(allowableValues = {"%", "$"})
            String printingDiscountType,
            BigDecimal printingDiscountValue,
            Boolean completed,
            List<PrintEntryRequest> entries
    ) {
    }

    @Schema(name = "ProductionOrderPrintEntryRequest")
    public record PrintEntryRequest(
            @JsonAlias("productionOrderPrintEntryId")
            String printEntryId,
            Integer shotsInkCount,
            List<String> shotsInks,
            Integer reverseInkCount,
            List<String> reverseInks,
            @Schema(allowableValues = {"no-flip", "gripper-flip", "square-flip"})
            String basicFlipType,
            String basicThousandRateId,
            @Schema(allowableValues = {"no-flip", "gripper-flip", "square-flip"})
            String pantoneFlipType,
            Boolean clientSuppliesPantoneInk,
            BigDecimal pantoneInkChargePrice,
            String pantoneThousandRateId
    ) {
    }

    @Schema(name = "UpdateProductionOrderPostpressRequest")
    public record UpdatePostpressRequest(
            @NotNull Long version,
            Boolean completed,
            @Valid List<OperatorRequest> operators,
            String operatorUserId,
            @Schema(allowableValues = {"%", "$"})
            String discountType,
            BigDecimal discountValue,
            @JsonAlias("postpressRecords")
            List<PostpressRecordRequest> records
    ) {
    }

    @Schema(name = "ProductionOrderPostpressRecordRequest")
    public record PostpressRecordRequest(
            @JsonAlias("productionOrderPostpressRecordId")
            String recordId,
            String plateId,
            Boolean completed,
            List<PostpressLineRequest> lines
    ) {
    }

    @Schema(name = "ProductionOrderPostpressLineRequest")
    public record PostpressLineRequest(
            @JsonAlias("productionOrderPostpressLineId")
            String lineId,
            String catalogItemId,
            @Schema(allowableValues = {"catalog", "quick-access"})
            String source,
            BigDecimal areaFactor,
            Integer goodSizes,
            Boolean positive,
            Boolean cliche
    ) {
    }

    @Schema(name = "UpdateProductionOrderBillingRequest")
    public record UpdateBillingRequest(
            @NotNull Long version,
            @Schema(allowableValues = {"%", "$"})
            String billingDiscountType,
            BigDecimal billingDiscountValue,
            @Schema(allowableValues = {"exact", "volume"})
            String clientCostingMode,
            @Schema(allowableValues = {"%", "$"})
            String clientDiscountType,
            BigDecimal clientDiscountValue,
            @Schema(allowableValues = {"%", "$"})
            String clientProfitabilityType,
            BigDecimal clientProfitabilityValue,
            List<Map<String, Object>> clientVolumeCosting,
            LocalDate deliveryStartDate,
            LocalDate deliveryEndDate,
            @Schema(example = "50.00")
            BigDecimal advancePercentage,
            String clientSignatureName,
            String bankAccountId,
            Boolean completed,
            @Valid List<OperatorRequest> operators,
            String operatorUserId
    ) {
    }
}
