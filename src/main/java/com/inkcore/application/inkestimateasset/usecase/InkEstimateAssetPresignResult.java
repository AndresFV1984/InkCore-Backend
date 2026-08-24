package com.inkcore.application.inkestimateasset.usecase;

import com.inkcore.domain.objectstorage.model.PresignedUpload;

public record InkEstimateAssetPresignResult(
        String objectKey,
        String previewObjectKey,
        String contentType,
        long sizeBytes,
        String fileName,
        PresignedUpload upload,
        PresignedUpload previewUpload
) {
}
