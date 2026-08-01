package com.inkcore.infrastructure.in.rest.papertypes;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(name = "PaperTypeCutLayoutRequest", description = "Despiece asociado al tipo de papel con valor de corte opcional")
public record PaperTypeCutLayoutRequest(
        @Schema(description = "Identificador del despiece", example = "714ad646-c4fe-42fa-9f13-4a44823e6bee", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El despiece es obligatorio")
        @Size(max = 64)
        String cutLayoutId,

        @Schema(description = "Valor de corte para este despiece (null si no aplica)", example = "200.00")
        @DecimalMin(value = "0.0", inclusive = true, message = "El valor de corte no puede ser negativo")
        BigDecimal cutValue
) {
}
