package com.inkcore.infrastructure.in.rest.inkestimateassets;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "InkEstimateAssetPresignRequest")
public record InkEstimateAssetPresignRequest(
        @Schema(description = "Plancha asociada", requiredMode = Schema.RequiredMode.REQUIRED, example = "plate-of-order-001")
        String plateId,
        @Schema(description = "Identificador de entrada en inkEstimation.entries", requiredMode = Schema.RequiredMode.REQUIRED, example = "entry-1")
        String entradaId,
        @Schema(example = "flyer.pdf")
        String fileName,
        @Schema(example = "application/pdf")
        String contentType,
        @Schema(example = "184320")
        Long sizeBytes,
        @Schema(example = "preview.jpg")
        String previewFileName,
        @Schema(example = "image/jpeg")
        String previewContentType,
        @Schema(example = "42000")
        Long previewSizeBytes,
        @Schema(description = "Si el original ya está en S3 y solo se pide miniatura")
        String existingObjectKey
) {
}
