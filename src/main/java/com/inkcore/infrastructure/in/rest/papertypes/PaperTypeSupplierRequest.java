package com.inkcore.infrastructure.in.rest.papertypes;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(
        name = "PaperTypeSupplierRequest",
        description = "Proveedor asociado al tipo de papel con valor hoja y unidad empaque"
)
public record PaperTypeSupplierRequest(
        @Schema(description = "Identificador del proveedor", example = "914ad646-c4fe-42fa-9f13-4a44823e6bee", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El proveedor es obligatorio")
        @Size(max = 64)
        String supplierId,

        @Schema(description = "Valor de la hoja/pliego para este proveedor", example = "1500.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El valor de la hoja es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El valor de la hoja no puede ser negativo")
        BigDecimal sheetValue,

        @Schema(description = "Hojas por unidad de empaque para este proveedor", example = "500", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La unidad de empaque es obligatoria")
        @Min(value = 1, message = "La unidad de empaque debe ser mayor que 0")
        Integer packageUnit
) {
}
