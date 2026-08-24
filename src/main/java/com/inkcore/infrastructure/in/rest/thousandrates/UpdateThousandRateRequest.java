package com.inkcore.infrastructure.in.rest.thousandrates;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(
        name = "UpdateThousandRateRequest",
        description = """
                Actualización de tarifa por millar (PUT /api/v1/thousand-rates/update/{thousandRateId}).
                Obligatorios: name, colorCategory, price, state, minThresholdUnits, minThousand, decimalThreshold, isDefault.
                Opcionales: thousandUnit, gripperFlipPrice, squareFlipPrice.
                No envía companyId.
                """
)
public record UpdateThousandRateRequest(
        @Schema(description = "Nombre de la tarifa", example = "Color básico", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150)
        String name,

        @Schema(description = "Categoría de color de la tarifa", example = "1 COLOR", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La categoría de color es obligatoria")
        @Size(max = 50)
        String colorCategory,

        @Schema(description = "Unidad de millar (default 1000)", example = "1000")
        @Min(value = 1, message = "La unidad millar debe ser mayor que 0")
        Integer thousandUnit,

        @Schema(description = "Precio de la tarifa por millar", example = "18000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El precio no puede ser negativo")
        BigDecimal price,

        @Schema(description = "Estado: true=activo, false=inactivo", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Boolean state,

        @Schema(description = "Cantidad mínima en unidades para aplicar reglas de cobro por millar", example = "600", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El tope mínimo millar es obligatorio")
        @Min(value = 1, message = "El tope mínimo millar debe ser mayor que 0")
        Integer minThresholdUnits,

        @Schema(description = "Millar mínimo de venta asociado a la tarifa", example = "500.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El millar mínimo es obligatorio")
        @DecimalMin(value = "0.01", inclusive = true, message = "El millar mínimo debe ser mayor que 0")
        BigDecimal minThousand,

        @Schema(description = "Umbral decimal (0 a 1)", example = "0.20", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El umbral decimal es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El umbral decimal debe estar entre 0 y 1")
        @DecimalMax(value = "1.0", inclusive = true, message = "El umbral decimal debe estar entre 0 y 1")
        BigDecimal decimalThreshold,

        @Schema(description = "Precio por millar con volteo por pinza; null si no aplica", example = "20000.00")
        @DecimalMin(value = "0.0", inclusive = true, message = "El precio de volteo por pinza no puede ser negativo")
        BigDecimal gripperFlipPrice,

        @Schema(description = "Precio por millar con volteo por escuadra; null si no aplica", example = "20000.00")
        @DecimalMin(value = "0.0", inclusive = true, message = "El precio de volteo por escuadra no puede ser negativo")
        BigDecimal squareFlipPrice,

        @Schema(description = "Indica si es la tarifa por defecto", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El indicador de tarifa por defecto es obligatorio")
        Boolean isDefault
) {
}
