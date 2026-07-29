package com.inkcore.infrastructure.in.rest.finishingprocesses;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(
        name = "UpdateFinishingProcessRequest",
        description = """
                Actualización de proceso de acabado (PUT /api/v1/finishing-processes/update/{finishingProcessId}).
                Obligatorios: name, quickAccess, state.
                Opcionales: minCost, valuePerCm2. No envía companyId.
                """
)
public record UpdateFinishingProcessRequest(
        @Schema(description = "Nombre del proceso de acabado", example = "Plegar", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150)
        String name,

        @Schema(description = "Costo mínimo (sin símbolo $). Null si no aplica", example = "5000.00")
        @DecimalMin(value = "0.0", inclusive = true, message = "El costo mínimo no puede ser negativo")
        BigDecimal minCost,

        @Schema(description = "Valor por cm² (sin símbolo $; máx 9999)", example = "1200.00")
        @DecimalMin(value = "0.0", inclusive = true, message = "El valor cm² no puede ser negativo")
        @DecimalMax(value = "9999.0", inclusive = true, message = "El valor cm² no puede superar 9999")
        BigDecimal valuePerCm2,

        @Schema(description = "Acceso rápido en órdenes de producción", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Boolean quickAccess,

        @Schema(description = "Estado: true=activo, false=inactivo", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Boolean state
) {
}
