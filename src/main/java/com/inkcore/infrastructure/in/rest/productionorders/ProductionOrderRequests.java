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
            name = "ProductionOrderOutsourcedStageRequest",
            description = "Etapa tercerizada. Cobro al cliente = sellPrice o vendorCost × (1 + markupPercent/100). "
                    + "null en el PUT del paso = no modifica esa etapa."
    )
    public record OutsourcedStageRequest(
            @Schema(
                    description = "Etapa",
                    example = "FINISHED_PRODUCTS",
                    allowableValues = {
                            "PREPRESS", "CUTTING", "PRINTING",
                            "FINISHED_PRODUCTS", "FINISHING_PROCESSES"
                    }
            )
            String stage,
            @Schema(description = "Si true, sustituye el cobro interno de la etapa en totalToCharge", example = "true")
            Boolean enabled,
            @Schema(description = "Costo pactado con el proveedor", example = "200000.00")
            BigDecimal vendorCost,
            @Schema(description = "Markup % sobre vendorCost", example = "50.00")
            BigDecimal markupPercent,
            @Schema(description = "Cobro al cliente (opcional; si se omite se calcula)", example = "300000.00")
            BigDecimal sellPrice,
            @Schema(description = "Id del proveedor en directorio (opcional)", example = "supplier-seed-001")
            String supplierId,
            @Schema(description = "Nombre del proveedor (opcional)", example = "Acabados XYZ")
            String supplierName,
            @Schema(description = "Fecha del acuerdo/cotización (YYYY-MM-DD)", example = "2026-10-01")
            java.time.LocalDate quotedAt,
            @Schema(description = "Cantidad a tercerizar", example = "1000")
            Integer quantity,
            @Schema(description = "Usuario litografía que autoriza/gestiona el costo", example = "user-001")
            String costResponsibleUserId,
            @Schema(description = "Nombre del responsable del costo", example = "Ana Pérez")
            String costResponsibleUserName,
            @Schema(description = "Vigencia de la cotización del proveedor", example = "2026-10-15")
            java.time.LocalDate quoteValidUntil,
            @Schema(description = "Fecha de envío al proveedor", example = "2026-10-20")
            java.time.LocalDate sentAt,
            @Schema(description = "Qué se entrega de la etapa al proveedor", example = "1200 pliegos impresos para barniz UV")
            String sentDescription,
            @Schema(description = "Fecha límite esperada de retorno", example = "2026-10-25")
            java.time.LocalDate expectedReturnAt,
            @Schema(description = "Fecha de recepción en planta", example = "2026-10-24")
            java.time.LocalDate receivedAt,
            @Schema(description = "Usuario (producción/admin) que registró la recepción", example = "user-admin")
            String receivedByUserId,
            @Schema(
                    description = "Estado operativo: PENDING|DISPATCHED|IN_PROGRESS|COMPLETED",
                    example = "PENDING",
                    allowableValues = {"PENDING", "DISPATCHED", "IN_PROGRESS", "COMPLETED"}
            )
            String fulfillmentStatus
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
            @Schema(description = "Operador opcional de la etapa PREPRESS (legacy)", example = "operator-seed-001")
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

    @Schema(
            name = "UpdateProductionOrderStatusRequest",
            description = "Cambio de estado de planta y/o baja lógica. "
                    + "Preferir status=ANULADA. CANCELLED es alias temporal de entrada (deprecated) y se persiste como ANULADA. "
                    + "Al pasar a cualquier IN_PROGRESS* se crea (idempotente) el pedido comercial customer_orders "
                    + "(customerOrderId + odpNumber=ODP-{n}) si aún no existe."
    )
    public record UpdateStatusRequest(
            @Schema(description = "Versión optimista actual", example = "6", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull Long version,
            @Schema(
                    description = "Estado de planta. ANULADA reemplaza CANCELLED (alias temporal de entrada: "
                            + "CANCELLED, CANCELED, CANCELADA, ANULADO — deprecated). La respuesta siempre usa ANULADA. "
                            + "IN_PROGRESS* dispara la creación del pedido comercial.",
                    example = "IN_PROGRESS",
                    allowableValues = {
                            "PENDING", "PAUSED", "UNDER_REVIEW", "IN_PROGRESS",
                            "IN_PROGRESS_PREPRESS", "IN_PROGRESS_CUTTING", "IN_PROGRESS_PRINTING",
                            "IN_PROGRESS_FINISHED_PRODUCTS", "IN_PROGRESS_FINISHING",
                            "COMPLETED", "ANULADA",
                            "CANCELLED"
                    }
            )
            String status,
            @Schema(description = "false = baja lógica (archivar)", example = "true")
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
            List<PlateRequest> plates,
            @Schema(description = "Máquinas de preprensa. null no modifica; [] quita las máquinas de la fase.")
            List<MachineUsageRequest> machineUsages,
            @Schema(description = "Porcentaje de merma operativa sobre las planchas. Si se omite, se usa el default de la compañía (sugerencia inicial 3%).", example = "3.00")
            BigDecimal plannedWastePercentage,
            @Schema(description = "Tercerización de preprensa. null = no modifica.")
            OutsourcedStageRequest outsourcedPricing
    ) {
    }

    @Schema(name = "ProductionOrderMachineUsageRequest")
    public record MachineUsageRequest(
            @Schema(
                    description = "Máquina activa de la fase (semilla impresión: 814ad646-…; "
                            + "preprensa/corte/terminados/acabados: machine-seed-*)",
                    example = "814ad646-c4fe-42fa-9f13-4a44823e6bee"
            )
            String machineId,
            @Schema(description = "Minutos estimados de arranque (default: setupMinutes del catálogo)", example = "15")
            Integer estimatedSetupMinutes,
            @Schema(description = "Minutos estimados de producción", example = "120")
            Integer estimatedRunMinutes
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
            List<PaperRowRequest> paperRows,
            @Schema(description = "Máquinas de corte de papel. null no modifica; [] quita las máquinas de la fase.")
            List<MachineUsageRequest> machineUsages,
            @Schema(description = "Pliegos fijos de arranque. Si se omite, se usan los de la compañía (inicial 0). Se suman al porcentaje, no lo reemplazan.", example = "0.00")
            BigDecimal plannedMakereadyQuantity,
            @Schema(description = "Tercerización de corte. null = no modifica.")
            OutsourcedStageRequest outsourcedPricing
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
            @Schema(description = "Papel del catálogo (FK papers)", example = "paper-seed-001")
            String paperId,
            @Schema(description = "Proveedor del precio vigente del papel", example = "supplier-seed-001")
            String supplierId,
            @Schema(description = "Despiece de catálogo (cut_layouts). Alternativa: paperCutLayoutId.",
                    example = "cut-layout-seed-001")
            String cutLayoutId,
            @Schema(description = "Asociación papel↔despiece; si se envía, resuelve cutLayoutId.",
                    example = "paper-cut-layout-001")
            String paperCutLayoutId,
            @Schema(description = "Regla de selección de proveedor",
                    allowableValues = {"PREFERRED", "REPLACEMENT", "BEST_COST"},
                    example = "PREFERRED")
            String priceRule,
            Boolean isPaperCut,
            Integer deliveredSheetsByClient,
            Integer manualGoodSizes,
            Integer manualSurplus,
            @Schema(description = "Merma de corte de la OP (company_waste_settings). "
                    + "NO enviar el wastePercentage del paper_cut_layout como si fuera cálculo: ese es solo sugerencia UI. "
                    + "Si se omite, se usa el default de la compañía (sugerencia inicial ~2%).",
                    example = "2.00")
            BigDecimal plannedWastePercentage,
            @Schema(description = "Remanente del mismo papel usado como origen del corte. "
                    + "Al guardar se descuenta calculatedSheetsCount de quantity_available; al anular la OP se devuelve.",
                    example = "paper-remnant-seed-001")
            String paperRemnantId
    ) {
    }

    @Schema(name = "UpdateProductionOrderPrintingRequest")
    public record UpdatePrintingRequest(
            @NotNull Long version,
            Boolean completed,
            @Valid List<OperatorRequest> operators,
            String operatorUserId,
            @Schema(description = "Porcentaje de merma operativa. Si se omite, se usa el default de la compañía (sugerencia inicial 3%).", example = "3.00")
            BigDecimal plannedOperationalWastePercentage,
            @Schema(description = "Máquinas de impresión. null no modifica; [] quita las máquinas de la fase.")
            List<MachineUsageRequest> machineUsages,
            List<PrintRequest> prints,
            @Schema(description = "Pliegos fijos de arranque. Si se omite, se usan los de la compañía (inicial 0). Se suman al porcentaje, no lo reemplazan.", example = "0.00")
            BigDecimal plannedMakereadyQuantity,
            @Schema(description = "Tercerización de impresión. null = no modifica.")
            OutsourcedStageRequest outsourcedPricing
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
            List<PostpressRecordRequest> records,
            @Schema(description = "Máquinas de la fase (terminados o acabados). null no modifica; [] quita las máquinas de la fase.")
            List<MachineUsageRequest> machineUsages,
            @Schema(description = "Porcentaje de merma operativa sobre las piezas. Si se omite, se usa el default de la compañía (sugerencia inicial 3%).", example = "3.00")
            BigDecimal plannedWastePercentage,
            @Schema(description = "Tercerización de terminados/acabados. null = no modifica. Si stage se omite, se infiere del endpoint.")
            OutsourcedStageRequest outsourcedPricing
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
            String operatorUserId,
            @Schema(description = "Replace-all de etapas tercerizadas. null = no modifica; [] limpia todas.")
            @Valid List<OutsourcedStageRequest> outsourcedStages
    ) {
    }

    @Schema(name = "ReprintWasteRequest")
    public record ReprintWasteRequest(
            @NotBlank
            @Schema(
                    description = "Fase en la que se paga el retrabajo",
                    allowableValues = {"preprensa", "corte-papel", "impresion", "terminados", "acabados"},
                    example = "impresion"
            )
            String phase,
            @NotNull
            @Positive
            @Schema(description = "Cantidad rehecha, mayor que cero", example = "50.00")
            BigDecimal quantity,
            @NotBlank
            @Schema(
                    description = "Motivo del retrabajo. Se guarda la etiqueta en note.",
                    allowableValues = {
                            "papel_mala_calidad", "mal_cortado", "ajuste_registro_color",
                            "cambio_medio_tiro", "defecto_impresion", "otro"
                    },
                    example = "defecto_impresion"
            )
            String wasteReason,
            @Schema(description = "Máquina de la fase, si se quiere validar que está cotizada. Opcional.")
            String machineId
    ) {
    }

    @Schema(
            name = "ReorderProductionOrdersRequest",
            description = "Cola de trabajo de la orden completa, de la primera a la última. "
                    + "Debe incluir todas las órdenes abiertas de la empresa."
    )
    public record ReorderRequest(
            @NotNull
            @Schema(
                    description = "Identificadores en el orden de trabajo. El primero es la siguiente orden.",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            List<@NotBlank String> productionOrderIds
    ) {
    }

    @Schema(
            name = "ReorderProductionOrderStageRequest",
            description = "Cola de una etapa para las órdenes en proceso, de la primera a la última."
    )
    public record ReorderStageRequest(
            @NotBlank
            @Schema(
                    description = "Etapa: preprensa, corte-papel, impresion, terminados o acabados.",
                    allowableValues = {"preprensa", "corte-papel", "impresion", "terminados", "acabados"},
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            String stage,
            @NotNull
            @Schema(
                    description = "Órdenes en proceso, en el turno de esa etapa. El primero es el siguiente.",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            List<@NotBlank String> productionOrderIds
    ) {
    }

    @Schema(name = "ProductionOrderStagePriority", description = "Turno de una etapa para una orden en proceso.")
    public record StagePriorityResponse(
            String productionOrderId,
            @Schema(description = "Etapa: preprensa, corte-papel, impresion, terminados o acabados.")
            String stage,
            int priorityRank
    ) {
        public static StagePriorityResponse from(
                com.inkcore.domain.productionorder.ports.out.ProductionOrderStagePriorityRepositoryPort.StagePriority row
        ) {
            return new StagePriorityResponse(row.productionOrderId(), row.stageCode(), row.priorityRank());
        }
    }
}
