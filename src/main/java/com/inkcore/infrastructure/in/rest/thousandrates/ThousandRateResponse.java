package com.inkcore.infrastructure.in.rest.thousandrates;

import com.inkcore.domain.thousandrate.model.ThousandRate;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "ThousandRateResponse", description = "Tarifa por millar del catálogo (formulario Nueva tarifa por millar)")
public record ThousandRateResponse(
        @Schema(description = "Identificador único de la tarifa", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String thousandRateId,

        @Schema(description = "Identificador de empresa", example = "company-seed-001")
        String companyId,

        @Schema(description = "Nombre de la tarifa", example = "Color básico")
        String name,

        @Schema(description = "Categoría de color de la tarifa", example = "1 COLOR")
        String colorCategory,

        @Schema(description = "Unidad de millar", example = "1000")
        int thousandUnit,

        @Schema(description = "Precio de la tarifa por millar", example = "17500.00")
        BigDecimal price,

        @Schema(description = "true = Activo, false = Inactivo", example = "true")
        boolean state,

        @Schema(description = "Cantidad mínima en unidades para aplicar reglas de cobro por millar", example = "600")
        int minThresholdUnits,

        @Schema(description = "Millar mínimo de venta asociado a la tarifa", example = "500.00")
        BigDecimal minThousand,

        @Schema(description = "Umbral decimal (0 a 1)", example = "0.20")
        BigDecimal decimalThreshold,

        @Schema(description = "Precio por millar con volteo por pinza; null si no aplica", example = "20000.00")
        BigDecimal gripperFlipPrice,

        @Schema(description = "Precio por millar con volteo por escuadra; null si no aplica", example = "20000.00")
        BigDecimal squareFlipPrice,

        @Schema(description = "Indica si es la tarifa por defecto", example = "false")
        boolean isDefault,

        @Schema(description = "Fecha de registro", example = "2026-08-08")
        LocalDate creationDate
) {
    public static ThousandRateResponse from(ThousandRate t) {
        return new ThousandRateResponse(
                t.getThousandRateId(),
                t.getCompanyId(),
                t.getName(),
                t.getColorCategory(),
                t.getThousandUnit(),
                t.getPrice(),
                t.isState(),
                t.getMinThresholdUnits(),
                t.getMinThousand(),
                t.getDecimalThreshold(),
                t.getGripperFlipPrice(),
                t.getSquareFlipPrice(),
                t.isDefault(),
                t.getCreationDate()
        );
    }
}
