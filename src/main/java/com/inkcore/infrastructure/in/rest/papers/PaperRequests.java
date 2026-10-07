package com.inkcore.infrastructure.in.rest.papers;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class PaperRequests {

    private PaperRequests() {
    }

    @Schema(name = "CreatePaperRequest", description = "Alta de papel (POST /api/v1/papers/register). "
            + "Unicidad: company + name + grammage + width + height + unit.")
    public record CreatePaperRequest(
            @NotBlank
            @Schema(description = "Nombre comercial", example = "Bond 75", requiredMode = Schema.RequiredMode.REQUIRED)
            String name,
            @Schema(description = "Gramaje g/m² (opcional)", example = "75.00")
            BigDecimal grammage,
            @NotNull
            @Schema(description = "Ancho del formato de compra", example = "70.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal width,
            @NotNull
            @Schema(description = "Alto del formato de compra", example = "100.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal height,
            @Schema(description = "Unidad de medida del formato: cm|mm|in (default cm)", example = "cm",
                    allowableValues = {"cm", "mm", "in"})
            String unit,
            @Schema(description = "true = papel esmaltado/estucado (vive en el papel, no en el precio)", example = "false")
            Boolean coated,
            @Schema(description = "Guía UI: true = papel candidable a remanentes (default false). "
                    + "No bloquea el API de remanentes.", example = "true")
            Boolean acceptsRemnants,
            @Schema(description = "Guía UI: ancho mínimo sugerido de remanente. "
                    + "Obligatorio en el papel si acceptsRemnants=true; no se aplica en POST remanentes.",
                    example = "20.00")
            BigDecimal minRemnantWidth,
            @Schema(description = "Guía UI: alto mínimo sugerido de remanente. "
                    + "Obligatorio en el papel si acceptsRemnants=true; no se aplica en POST remanentes.",
                    example = "20.00")
            BigDecimal minRemnantHeight,
            @Schema(description = "Unidad de las mínimas de remanente: cm|mm|in. "
                    + "Obligatoria si acceptsRemnants=true (default = unit del pliego si se omite).",
                    example = "cm", allowableValues = {"cm", "mm", "in"})
            String minRemnantUnit,
            @Schema(description = "true = activo (default true)", example = "true")
            Boolean state
    ) {
    }

    @Schema(name = "UpdatePaperRequest", description = "Actualización de papel (PUT /api/v1/papers/update/{paperId})")
    public record UpdatePaperRequest(
            @NotBlank
            @Schema(description = "Nombre comercial", example = "Bond 75", requiredMode = Schema.RequiredMode.REQUIRED)
            String name,
            @Schema(description = "Gramaje g/m² (opcional)", example = "75.00")
            BigDecimal grammage,
            @NotNull
            @Schema(description = "Ancho del formato de compra", example = "70.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal width,
            @NotNull
            @Schema(description = "Alto del formato de compra", example = "100.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal height,
            @Schema(description = "Unidad del pliego de compra: cm|mm|in", example = "cm",
                    allowableValues = {"cm", "mm", "in"})
            String unit,
            @Schema(description = "true = papel esmaltado/estucado", example = "false")
            Boolean coated,
            @Schema(description = "Guía UI: true = papel candidable a remanentes", example = "true")
            Boolean acceptsRemnants,
            @Schema(description = "Guía UI: ancho mínimo sugerido de remanente. Obligatorio si acceptsRemnants=true",
                    example = "20.00")
            BigDecimal minRemnantWidth,
            @Schema(description = "Guía UI: alto mínimo sugerido de remanente. Obligatorio si acceptsRemnants=true",
                    example = "20.00")
            BigDecimal minRemnantHeight,
            @Schema(description = "Unidad de las mínimas de remanente: cm|mm|in. Obligatoria si acceptsRemnants=true",
                    example = "cm", allowableValues = {"cm", "mm", "in"})
            String minRemnantUnit,
            @Schema(description = "true = activo, false = inactivo", example = "true")
            Boolean state
    ) {
    }

    @Schema(name = "ReplacePaperPricesRequest",
            description = "Reemplazo total de precios vigentes del papel (PUT /api/v1/papers/{paperId}/prices). "
                    + "Solo un preferred=true por papel. packageUnit es obligatorio por proveedor.")
    public record ReplacePaperPricesRequest(
            @Valid
            @Schema(description = "Lista completa de precios a dejar vigentes", requiredMode = Schema.RequiredMode.REQUIRED)
            List<ReplacePaperPriceItemRequest> prices
    ) {
    }

    @Schema(name = "ReplacePaperPriceItemRequest", description = "Precio vigente por proveedor")
    public record ReplacePaperPriceItemRequest(
            @NotBlank
            @Schema(description = "Proveedor", example = "supplier-seed-001", requiredMode = Schema.RequiredMode.REQUIRED)
            String supplierId,
            @NotNull
            @Schema(description = "Precio por pliego (> 0)", example = "1500.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal sheetValue,
            @NotNull
            @Schema(description = "Unidad de empaque: pliegos por empaque del proveedor (> 0)", example = "500",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            Integer packageUnit,
            @Schema(description = "Flete por pliego (>= 0; default 0)", example = "0.00")
            BigDecimal freightPerSheet,
            @Schema(description = "Mínimo de pliegos de compra; null si no aplica", example = "500")
            Integer minPurchaseSheets,
            @Schema(description = "Plazo de pago en días", example = "30")
            Integer paymentDays,
            @Schema(description = "Días de entrega estimados", example = "5")
            Integer deliveryDays,
            @Schema(description = "Fecha de vigencia del precio", example = "2026-10-06")
            LocalDate priceDate,
            @Schema(description = "Proveedor preferido (solo uno activo por papel)", example = "true")
            Boolean preferred,
            @Schema(description = "true = activo", example = "true")
            Boolean state
    ) {
    }

    @Schema(name = "CreatePaperCutLayoutRequest",
            description = "Asocia un despiece del catálogo al papel. "
                    + "wastePercentage es SOLO sugerencia de UI; la merma de OP usa company_waste_settings + plannedWastePercentage.")
    public record CreatePaperCutLayoutRequest(
            @NotBlank
            @Schema(description = "Despiece de catálogo (cut_layouts)", example = "cut-layout-seed-001",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            String cutLayoutId,
            @Schema(description = "Orientación", example = "vertical", allowableValues = {"vertical", "horizontal"})
            String orientation,
            @Schema(description = "Sugerencia UI de desperdicio % (0..100). No se usa en el cálculo de OP.",
                    example = "2.00")
            BigDecimal wastePercentage,
            @Schema(description = "Nota libre", example = "Para etiquetas 10x5")
            String note,
            @Schema(description = "true = activo (default true)", example = "true")
            Boolean state
    ) {
    }

    @Schema(name = "UpdatePaperCutLayoutRequest",
            description = "Actualiza asociación papel↔despiece. No cambia cutLayoutId (borrar y crear para cambiar).")
    public record UpdatePaperCutLayoutRequest(
            @Schema(description = "Orientación", example = "horizontal", allowableValues = {"vertical", "horizontal"})
            String orientation,
            @Schema(description = "Sugerencia UI de desperdicio % (0..100). No se usa en el cálculo de OP.",
                    example = "2.50")
            BigDecimal wastePercentage,
            @Schema(description = "Nota libre")
            String note,
            @Schema(description = "true = activo", example = "true")
            Boolean state
    ) {
    }

    @Schema(name = "CreatePaperStockRequest", description = "Alta de lote de inventario del papel")
    public record CreatePaperStockRequest(
            @NotNull
            @Schema(description = "Cantidad inicial del lote", example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal quantityInitial,
            @NotNull
            @Schema(description = "Cantidad disponible (<= inicial)", example = "1000.00",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal quantityAvailable,
            @NotNull
            @Schema(description = "Costo unitario del pliego en este lote", example = "1500.00",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal unitCost,
            @Schema(description = "Fecha de ingreso (default hoy)", example = "2026-10-06")
            LocalDate entryDate,
            @Schema(description = "true = activo/disponible (default true)", example = "true")
            Boolean state
    ) {
    }

    @Schema(name = "UpdatePaperStockRequest", description = "Actualización de lote de inventario")
    public record UpdatePaperStockRequest(
            @NotNull
            @Schema(description = "Cantidad inicial del lote", example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal quantityInitial,
            @NotNull
            @Schema(description = "Cantidad disponible (<= inicial)", example = "850.00",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal quantityAvailable,
            @NotNull
            @Schema(description = "Costo unitario del pliego", example = "1500.00",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal unitCost,
            @Schema(description = "Fecha de ingreso", example = "2026-10-06")
            LocalDate entryDate,
            @Schema(description = "true = activo/disponible", example = "true")
            Boolean state
    ) {
    }

    @Schema(name = "CreatePaperRemnantRequest",
            description = "Alta de remanente reutilizable: mismo papel (material) con medidas propias del sobrante.")
    public record CreatePaperRemnantRequest(
            @NotNull
            @Schema(description = "Ancho del remanente", example = "35.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal width,
            @NotNull
            @Schema(description = "Alto del remanente", example = "50.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal height,
            @Schema(description = "Unidad: cm|mm|in (default cm)", example = "cm",
                    allowableValues = {"cm", "mm", "in"})
            String unit,
            @NotNull
            @Schema(description = "Cantidad inicial", example = "12.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal quantityInitial,
            @Schema(description = "Cantidad disponible (<= inicial; default = inicial)", example = "12.00")
            BigDecimal quantityAvailable,
            @Schema(description = "Costo unitario (>= 0; default 0)", example = "0.00")
            BigDecimal unitCost,
            @Schema(description = "OP origen (opcional)", example = "production-order-seed-001")
            String sourceProductionOrderId,
            @Schema(description = "Fila de corte origen (opcional)", example = "paper-row-seed-001")
            String sourcePaperRowId,
            @Schema(description = "Fecha de ingreso (default hoy)", example = "2026-10-06")
            LocalDate entryDate,
            @Schema(description = "Nota libre", example = "Sobrante de corte Bond 70x100")
            String note,
            @Schema(description = "true = activo/disponible (default true)", example = "true")
            Boolean state
    ) {
    }

    @Schema(name = "UpdatePaperRemnantRequest", description = "Actualización de remanente reutilizable")
    public record UpdatePaperRemnantRequest(
            @NotNull
            @Schema(description = "Ancho del remanente", example = "35.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal width,
            @NotNull
            @Schema(description = "Alto del remanente", example = "50.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal height,
            @Schema(description = "Unidad: cm|mm|in", example = "cm",
                    allowableValues = {"cm", "mm", "in"})
            String unit,
            @NotNull
            @Schema(description = "Cantidad inicial", example = "12.00", requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal quantityInitial,
            @NotNull
            @Schema(description = "Cantidad disponible (<= inicial)", example = "10.00",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            BigDecimal quantityAvailable,
            @Schema(description = "Costo unitario (>= 0; default 0)", example = "0.00")
            BigDecimal unitCost,
            @Schema(description = "OP origen (opcional)")
            String sourceProductionOrderId,
            @Schema(description = "Fila de corte origen (opcional)")
            String sourcePaperRowId,
            @Schema(description = "Fecha de ingreso", example = "2026-10-06")
            LocalDate entryDate,
            @Schema(description = "Nota libre")
            String note,
            @Schema(description = "true = activo/disponible", example = "true")
            Boolean state
    ) {
    }
}
