package com.inkcore.infrastructure.in.rest.finishes;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(
        name = "CreateFinishedProductRequest",
        description = """
                Alta de producto terminado (POST /api/v1/finished-products/register).
                Obligatorios: companyId, name.
                Opcionales: minCost, valuePerCm2 (default 0), quickAccess (default false), state (default true).
                """
)
public record CreateFinishRequest(
        @Schema(description = "Identificador de empresa", example = "company-seed-001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La empresa es obligatoria")
        @Size(max = 64)
        String companyId,

        @Schema(description = "Nombre del terminado", example = "Laminado mate", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150)
        String name,

        @Schema(description = "Costo mínimo (sin símbolo $). Null si no aplica", example = "15000.00")
        @DecimalMin(value = "0.0", inclusive = true, message = "El costo mínimo no puede ser negativo")
        BigDecimal minCost,

        @Schema(description = "Valor por cm² (sin símbolo $; default 0; máx 9999)", example = "5000.00")
        @DecimalMin(value = "0.0", inclusive = true, message = "El valor cm² no puede ser negativo")
        @DecimalMax(value = "9999.0", inclusive = true, message = "El valor cm² no puede superar 9999")
        BigDecimal valuePerCm2,

        @Schema(description = "Acceso rápido en órdenes de producción (default false)", example = "true")
        Boolean quickAccess,

        @Schema(description = "Estado: true=activo, false=inactivo (default true)", example = "true")
        Boolean state
) {
}
