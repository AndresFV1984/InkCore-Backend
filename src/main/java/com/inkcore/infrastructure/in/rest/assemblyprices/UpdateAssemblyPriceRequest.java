package com.inkcore.infrastructure.in.rest.assemblyprices;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(
        name = "UpdateAssemblyPriceRequest",
        description = """
                Actualización de precio de montaje (PUT /api/v1/assembly-prices/update/{assemblyPriceId}).
                Obligatorios: name, cost, state.
                No envía companyId.
                """
)
public record UpdateAssemblyPriceRequest(
        @Schema(description = "Nombre del precio de montaje", example = "Montaje estándar 4 tintas", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150)
        String name,

        @Schema(description = "Costo del montaje", example = "90000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El costo es obligatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "El costo no puede ser negativo")
        BigDecimal cost,

        @Schema(description = "Estado: true=activo, false=inactivo", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Boolean state
) {
}
