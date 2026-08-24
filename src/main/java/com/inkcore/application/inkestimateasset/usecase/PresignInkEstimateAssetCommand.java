package com.inkcore.application.inkestimateasset.usecase;

public record PresignInkEstimateAssetCommand(
        String plateId,
        String entradaId,
        String fileName,
        String contentType,
        Long sizeBytes,
        String previewFileName,
        String previewContentType,
        Long previewSizeBytes,
        String existingObjectKey
) {
}
