package com.inkcore.domain.colorconversion.model;

/**
 * Resultado de imagen→PDF CMYK.
 *
 * @param pdfBytes           PDF CMYK (ICCBased + OutputIntent)
 * @param previewJpeg        soft-proof RGB JPEG para UI (obligatorio comparar aquí, no el PDF crudo)
 * @param softProofLumaRatio luma(soft-proof) / luma(target creativo); ideal ~0.95–1.05. Null si no aplica.
 */
public record CmykPdfConversion(byte[] pdfBytes, byte[] previewJpeg, Double softProofLumaRatio) {
}
