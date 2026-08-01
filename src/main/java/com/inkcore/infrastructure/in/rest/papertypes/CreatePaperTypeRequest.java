package com.inkcore.infrastructure.in.rest.papertypes;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(
        name = "CreatePaperTypeRequest",
        description = """
                Alta de tipo de papel (POST /api/v1/paper-types/register).
                Obligatorios: companyId, name, width, height, sheetValue, packageUnit.
                Opcionales: unit (default cm), isCoated (default false), state (default true), cutLayouts.
                """
)
public record CreatePaperTypeRequest(
        @Schema(description = "Identificador de empresa", example = "company-seed-001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La empresa es obligatoria")
        @Size(max = 64)
        String companyId,

        @Schema(description = "Nombre del tipo de papel", example = "Bond 75g", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150)
        String name,

        @Schema(description = "Ancho de la hoja/pliego", example = "70.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El ancho es obligatorio")
        @DecimalMin(value = "0.01", inclusive = true, message = "El ancho debe ser mayor que 0")
        BigDecimal width,

        @Schema(description = "Alto de la hoja/pliego", example = "100.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El alto es obligatorio")
        @DecimalMin(value = "0.01", inclusive = true, message = "El alto debe ser mayor que 0")
        BigDecimal height,

        @Schema(description = "Unidad de medida: cm, mm o in (default cm)", example = "cm")
        @Pattern(regexp = "(?i)cm|mm|in", message = "La unidad debe ser cm, mm o in")
        String unit,

        @Schema(description = "Valor de la hoja/pliego", example = "1500.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El valor de la hoja es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El valor de la hoja no puede ser negativo")
        BigDecimal sheetValue,

        @Schema(description = "Hojas por unidad de empaque", example = "500", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La unidad de empaque es obligatoria")
        @Min(value = 1, message = "La unidad de empaque debe ser mayor que 0")
        Integer packageUnit,

        @Schema(description = "true = papel esmaltado (default false)", example = "false")
        Boolean isCoated,

        @Schema(description = "Estado: true=activo, false=inactivo (default true)", example = "true")
        Boolean state,

        @Schema(description = "Despieces asociados con valor de corte opcional")
        @Valid
        List<PaperTypeCutLayoutRequest> cutLayouts
) {
}
