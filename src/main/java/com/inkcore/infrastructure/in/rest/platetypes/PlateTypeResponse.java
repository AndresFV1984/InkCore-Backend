package com.inkcore.infrastructure.in.rest.platetypes;

import com.inkcore.domain.platetype.model.PlateType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "PlateTypeResponse", description = "Tipo de plancha del catálogo (formulario Nuevo tipo de plancha)")
public record PlateTypeResponse(
        @Schema(description = "Identificador único del tipo de plancha", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String plateTypeId,

        @Schema(description = "Identificador de empresa", example = "company-seed-001")
        String companyId,

        @Schema(description = "Nombre del tipo de plancha", example = "Plancha estándar")
        String name,

        @Schema(description = "Ancho de la plancha", example = "10.00")
        BigDecimal width,

        @Schema(description = "Alto de la plancha", example = "5.00")
        BigDecimal height,

        @Schema(description = "Unidad de medida", example = "cm", allowableValues = {"cm", "mm", "in"})
        String unit,

        @Schema(description = "Valor en pesos colombianos (COP)", example = "185000.00")
        BigDecimal value,

        @Schema(description = "true = Activo, false = Inactivo", example = "true")
        boolean state,

        @Schema(description = "Fecha de registro", example = "2026-08-04")
        LocalDate creationDate
) {
    public static PlateTypeResponse from(PlateType p) {
        return new PlateTypeResponse(
                p.getPlateTypeId(),
                p.getCompanyId(),
                p.getName(),
                p.getWidth(),
                p.getHeight(),
                p.getUnit(),
                p.getValue(),
                p.isState(),
                p.getCreationDate()
        );
    }
}
