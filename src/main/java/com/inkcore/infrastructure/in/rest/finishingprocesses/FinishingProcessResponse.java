package com.inkcore.infrastructure.in.rest.finishingprocesses;

import com.inkcore.domain.finishingprocess.model.FinishingProcess;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "FinishingProcessResponse", description = "Proceso de acabado del catálogo (formulario Nueva operación de acabado)")
public record FinishingProcessResponse(
        @Schema(description = "Identificador único del proceso de acabado", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String finishingProcessId,

        @Schema(description = "Identificador de empresa", example = "company-seed-001")
        String companyId,

        @Schema(description = "Nombre del proceso de acabado", example = "Plegar")
        String name,

        @Schema(description = "Costo mínimo; null si no aplica", example = "5000.00")
        BigDecimal minCost,

        @Schema(description = "Valor por cm²", example = "1200.00")
        BigDecimal valuePerCm2,

        @Schema(description = "true = visible en selección rápida de producción", example = "true")
        boolean quickAccess,

        @Schema(description = "true = Activo, false = Inactivo", example = "true")
        boolean state,

        @Schema(description = "Fecha de registro", example = "2026-07-28")
        LocalDate creationDate
) {
    public static FinishingProcessResponse from(FinishingProcess p) {
        return new FinishingProcessResponse(
                p.getFinishingProcessId(),
                p.getCompanyId(),
                p.getName(),
                p.getMinCost(),
                p.getValuePerCm2(),
                p.isQuickAccess(),
                p.isState(),
                p.getCreationDate()
        );
    }
}
