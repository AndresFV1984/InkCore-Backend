package com.inkcore.infrastructure.in.rest.papers;

import com.inkcore.domain.paper.model.Paper;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(name = "PaperResponse", description = "Papel del catálogo: material + formato de compra + política de remanentes")
public record PaperResponse(
        @Schema(description = "Identificador del papel", example = "paper-seed-001")
        String paperId,
        @Schema(description = "Identificador de empresa", example = "company-seed-001")
        String companyId,
        @Schema(description = "Nombre comercial", example = "Bond 75")
        String name,
        @Schema(description = "Gramaje g/m²; null si aún no se conoce", example = "75.00")
        BigDecimal grammage,
        @Schema(description = "Ancho del formato de compra", example = "70.00")
        BigDecimal width,
        @Schema(description = "Alto del formato de compra", example = "100.00")
        BigDecimal height,
        @Schema(description = "Unidad del pliego de compra: cm|mm|in", example = "cm",
                allowableValues = {"cm", "mm", "in"})
        String unit,
        @Schema(description = "true = papel esmaltado/estucado", example = "false")
        boolean coated,
        @Schema(description = "Guía UI: papel candidable a remanentes (el API de remanentes no lo bloquea)",
                example = "true")
        boolean acceptsRemnants,
        @Schema(description = "Guía UI: ancho mínimo sugerido de remanente; null si acceptsRemnants=false",
                example = "20.00")
        BigDecimal minRemnantWidth,
        @Schema(description = "Guía UI: alto mínimo sugerido de remanente; null si acceptsRemnants=false",
                example = "20.00")
        BigDecimal minRemnantHeight,
        @Schema(description = "Unidad de las mínimas de remanente; null si acceptsRemnants=false",
                example = "cm", allowableValues = {"cm", "mm", "in"})
        String minRemnantUnit,
        @Schema(description = "true = activo, false = inactivo", example = "true")
        boolean state,
        @Schema(description = "Fecha de registro", example = "2026-08-04")
        LocalDate creationDate,
        @Schema(description = "Última actualización (sin zona)", example = "2026-08-04T12:00:00")
        LocalDateTime updatedAt
) {
    public static PaperResponse from(Paper paper) {
        return new PaperResponse(
                paper.getPaperId(),
                paper.getCompanyId(),
                paper.getName(),
                paper.getGrammage(),
                paper.getWidth(),
                paper.getHeight(),
                paper.getUnit(),
                paper.isCoated(),
                paper.isAcceptsRemnants(),
                paper.getMinRemnantWidth(),
                paper.getMinRemnantHeight(),
                paper.getMinRemnantUnit(),
                paper.isState(),
                paper.getCreationDate(),
                paper.getUpdatedAt()
        );
    }
}
