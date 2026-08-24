package com.inkcore.infrastructure.in.rest.platetypes;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(
        name = "CreatePlateTypeRequest",
        description = """
                Alta de tipo de plancha (POST /api/v1/plate-types/register).
                Obligatorios: companyId, name, width, height, value.
                Opcionales: unit (default cm), state (default true).
                """
)
public record CreatePlateTypeRequest(
        @Schema(description = "Identificador de empresa", example = "company-seed-001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La empresa es obligatoria")
        @Size(max = 64)
        String companyId,

        @Schema(description = "Nombre del tipo de plancha", example = "Plancha estándar", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150)
        String name,

        @Schema(description = "Ancho de la plancha", example = "10.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El ancho es obligatorio")
        @DecimalMin(value = "0.01", inclusive = true, message = "El ancho debe ser mayor que 0")
        BigDecimal width,

        @Schema(description = "Alto de la plancha", example = "5.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El alto es obligatorio")
        @DecimalMin(value = "0.01", inclusive = true, message = "El alto debe ser mayor que 0")
        BigDecimal height,

        @Schema(description = "Unidad de medida: cm, mm o in (default cm)", example = "cm", allowableValues = {"cm", "mm", "in"})
        @Pattern(regexp = "(?i)cm|mm|in", message = "La unidad debe ser cm, mm o in")
        String unit,

        @Schema(description = "Valor en pesos colombianos (COP)", example = "185000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El valor es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El valor no puede ser negativo")
        BigDecimal value,

        @Schema(description = "Estado: true=activo, false=inactivo (default true)", example = "true")
        Boolean state
) {
}
