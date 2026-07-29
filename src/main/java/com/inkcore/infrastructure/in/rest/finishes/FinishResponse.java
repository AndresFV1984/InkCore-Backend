package com.inkcore.infrastructure.in.rest.finishes;

import com.inkcore.domain.finish.model.Finish;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "FinishedProductResponse", description = "Producto terminado del catálogo (formulario Nuevo terminado)")
public record FinishResponse(
        @Schema(description = "Identificador único del producto terminado", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String finishedProductId,

        @Schema(description = "Identificador de empresa", example = "company-seed-001")
        String companyId,

        @Schema(description = "Nombre del terminado", example = "Laminado mate")
        String name,

        @Schema(description = "Costo mínimo; null si no aplica", example = "15000.00")
        BigDecimal minCost,

        @Schema(description = "Valor por cm²", example = "5000.00")
        BigDecimal valuePerCm2,

        @Schema(description = "true = visible en selección rápida de producción", example = "true")
        boolean quickAccess,

        @Schema(description = "true = Activo, false = Inactivo", example = "true")
        boolean state,

        @Schema(description = "Fecha de registro", example = "2026-07-28")
        LocalDate creationDate
) {
    public static FinishResponse from(Finish f) {
        return new FinishResponse(
                f.getFinishId(),
                f.getCompanyId(),
                f.getName(),
                f.getMinCost(),
                f.getValuePerCm2(),
                f.isQuickAccess(),
                f.isState(),
                f.getCreationDate()
        );
    }
}
