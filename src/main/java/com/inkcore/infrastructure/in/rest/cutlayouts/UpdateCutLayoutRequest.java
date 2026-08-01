package com.inkcore.infrastructure.in.rest.cutlayouts;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(
        name = "UpdateCutLayoutRequest",
        description = """
                Actualización de despiece (PUT /api/v1/cut-layouts/update/{cutLayoutId}).
                Obligatorios: name, width, height, unit, piecesPerSheet, state. No envía companyId.
                """
)
public record UpdateCutLayoutRequest(
        @Schema(description = "Nombre del despiece (sin medidas)", example = "Etiqueta", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150)
        String name,

        @Schema(description = "Ancho de la pieza", example = "10.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El ancho es obligatorio")
        @DecimalMin(value = "0.01", inclusive = true, message = "El ancho debe ser mayor que 0")
        BigDecimal width,

        @Schema(description = "Alto de la pieza", example = "5.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El alto es obligatorio")
        @DecimalMin(value = "0.01", inclusive = true, message = "El alto debe ser mayor que 0")
        BigDecimal height,

        @Schema(description = "Unidad de medida: cm, mm o in", example = "cm", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La unidad es obligatoria")
        @Pattern(regexp = "(?i)cm|mm|in", message = "La unidad debe ser cm, mm o in")
        String unit,

        @Schema(description = "Cantidad de piezas que caben en un pliego", example = "24", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Las piezas por pliego son obligatorias")
        @Min(value = 1, message = "Las piezas por pliego deben ser mayor que 0")
        Integer piecesPerSheet,

        @Schema(description = "Estado: true=activo, false=inactivo", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Boolean state
) {
}
