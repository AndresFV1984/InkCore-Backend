package com.inkcore.domain.colorconversion.model;

/**
 * TIFF CMYK de salida + preview RGB JPEG (soft-proof en memoria) + métrica de paridad luma.
 *
 * @param softProofLumaRatio luma(soft-proof) / luma(RGB original); ideal ~0.95–1.05. Null si no aplica.
 */
public record CmykTiffConversion(byte[] tiffBytes, byte[] previewJpeg, Double softProofLumaRatio) {
    public CmykTiffConversion {
        if (tiffBytes == null || tiffBytes.length == 0) {
            throw new IllegalArgumentException("tiffBytes vacío");
        }
    }

    /** Compatibilidad tests: sin métrica. */
    public CmykTiffConversion(byte[] tiffBytes, byte[] previewJpeg) {
        this(tiffBytes, previewJpeg, null);
    }
}
