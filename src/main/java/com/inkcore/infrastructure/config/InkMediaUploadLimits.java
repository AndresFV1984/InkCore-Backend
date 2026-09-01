package com.inkcore.infrastructure.config;

import org.springframework.stereotype.Component;

/**
 * Tope unificado para estimación multipart y presign/PUT de artes de tinta.
 * Evita estimar archivos que luego no pueden subirse a MinIO.
 */
@Component
public class InkMediaUploadLimits {

    private final InkEstimationProperties inkEstimation;
    private final ObjectStorageProperties objectStorage;

    public InkMediaUploadLimits(
            InkEstimationProperties inkEstimation,
            ObjectStorageProperties objectStorage
    ) {
        this.inkEstimation = inkEstimation;
        this.objectStorage = objectStorage;
    }

    public long effectiveMaxOriginalBytes() {
        return Math.min(
                inkEstimation.getAbsoluteMaxFileBytes(),
                objectStorage.getMaxAssetFileBytes()
        );
    }
}
