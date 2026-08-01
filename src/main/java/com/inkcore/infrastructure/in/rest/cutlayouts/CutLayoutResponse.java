package com.inkcore.infrastructure.in.rest.cutlayouts;

import com.inkcore.domain.cutlayout.model.CutLayout;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(name = "CutLayoutResponse", description = "Despiece del catálogo (formulario Nuevo despiece)")
public record CutLayoutResponse(
        @Schema(description = "Identificador único del despiece", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String cutLayoutId,

        @Schema(description = "Identificador de empresa", example = "company-seed-001")
        String companyId,

        @Schema(description = "Nombre del despiece", example = "Etiqueta")
        String name,

        @Schema(description = "Ancho de la pieza", example = "10.00")
        BigDecimal width,

        @Schema(description = "Alto de la pieza", example = "5.00")
        BigDecimal height,

        @Schema(description = "Unidad de medida", example = "cm")
        String unit,

        @Schema(description = "Piezas por pliego", example = "24")
        int piecesPerSheet,

        @Schema(description = "true = Activo, false = Inactivo", example = "true")
        boolean state,

        @Schema(description = "Fecha de registro", example = "2026-08-01")
        LocalDate creationDate
) {
    public static CutLayoutResponse from(CutLayout c) {
        return new CutLayoutResponse(
                c.getCutLayoutId(),
                c.getCompanyId(),
                c.getName(),
                c.getWidth(),
                c.getHeight(),
                c.getUnit(),
                c.getPiecesPerSheet(),
                c.isState(),
                c.getCreationDate()
        );
    }
}
