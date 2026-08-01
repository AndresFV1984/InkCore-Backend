package com.inkcore.infrastructure.in.rest.papertypes;

import com.inkcore.domain.papertype.model.PaperType;
import com.inkcore.domain.papertype.model.PaperTypeCutAssignment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(name = "PaperTypeResponse", description = "Tipo de papel del catálogo (formulario Nuevo tipo de papel)")
public record PaperTypeResponse(
        @Schema(description = "Identificador único del tipo de papel", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String paperTypeId,

        @Schema(description = "Identificador de empresa", example = "company-seed-001")
        String companyId,

        @Schema(description = "Nombre del tipo de papel", example = "Bond 75g")
        String name,

        @Schema(description = "Ancho de la hoja/pliego", example = "70.00")
        BigDecimal width,

        @Schema(description = "Alto de la hoja/pliego", example = "100.00")
        BigDecimal height,

        @Schema(description = "Unidad de medida", example = "cm")
        String unit,

        @Schema(description = "Valor de la hoja/pliego", example = "1500.00")
        BigDecimal sheetValue,

        @Schema(description = "Hojas por unidad de empaque", example = "500")
        int packageUnit,

        @Schema(description = "true = papel esmaltado", example = "false")
        boolean isCoated,

        @Schema(description = "true = Activo, false = Inactivo", example = "true")
        boolean state,

        @Schema(description = "Fecha de registro", example = "2026-08-01")
        LocalDate creationDate,

        @Schema(description = "Despieces asociados con valor de corte")
        List<PaperTypeCutLayoutResponse> cutLayouts
) {
    public static PaperTypeResponse from(PaperType p) {
        List<PaperTypeCutLayoutResponse> cuts = p.getCutAssignments().stream()
                .map(PaperTypeCutLayoutResponse::from)
                .toList();
        return new PaperTypeResponse(
                p.getPaperTypeId(),
                p.getCompanyId(),
                p.getName(),
                p.getWidth(),
                p.getHeight(),
                p.getUnit(),
                p.getSheetValue(),
                p.getPackageUnit(),
                p.isCoated(),
                p.isState(),
                p.getCreationDate(),
                cuts
        );
    }

    @Schema(name = "PaperTypeCutLayoutResponse", description = "Asignación despiece ↔ tipo de papel")
    public record PaperTypeCutLayoutResponse(
            @Schema(description = "Identificador del despiece", example = "714ad646-c4fe-42fa-9f13-4a44823e6bee")
            String cutLayoutId,

            @Schema(description = "Valor de corte; null si no aplica", example = "200.00")
            BigDecimal cutValue
    ) {
        public static PaperTypeCutLayoutResponse from(PaperTypeCutAssignment a) {
            return new PaperTypeCutLayoutResponse(a.getCutLayoutId(), a.getCutValue());
        }
    }
}
