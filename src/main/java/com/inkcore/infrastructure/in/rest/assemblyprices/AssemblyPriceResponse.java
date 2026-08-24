package com.inkcore.infrastructure.in.rest.assemblyprices;

import com.inkcore.domain.assemblyprice.model.AssemblyPrice;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "AssemblyPriceResponse", description = "Precio de montaje del catálogo (formulario Nuevo precio de montaje)")
public record AssemblyPriceResponse(
        @Schema(description = "Identificador único del precio de montaje", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String assemblyPriceId,

        @Schema(description = "Identificador de empresa", example = "company-seed-001")
        String companyId,

        @Schema(description = "Nombre del precio de montaje", example = "Montaje estándar 4 tintas")
        String name,

        @Schema(description = "Costo del montaje", example = "85000.00")
        BigDecimal cost,

        @Schema(description = "true = Activo, false = Inactivo", example = "true")
        boolean state,

        @Schema(description = "Fecha de registro", example = "2026-08-08")
        LocalDate creationDate
) {
    public static AssemblyPriceResponse from(AssemblyPrice p) {
        return new AssemblyPriceResponse(
                p.getAssemblyPriceId(),
                p.getCompanyId(),
                p.getName(),
                p.getCost(),
                p.isState(),
                p.getCreationDate()
        );
    }
}
