package com.inkcore.domain.colorconversion.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Formato de salida de la conversión CMYK.
 * Imagen de entrada: TIFF (default) o PDF.
 * PDF de entrada: PDF (default) o TIFF (rasterizado; pierde vectores).
 */
@Schema(
        name = "OutputFormat",
        description = "Formato de salida CMYK: TIFF o PDF"
)
public enum OutputFormat {
    @Schema(description = "TIFF CMYK LZW (default en imagen; también PDF→TIFF rasterizado)")
    TIFF,

    @Schema(description = "PDF CMYK con ICCBased + OutputIntent (default en PDF de entrada; también imagen→PDF)")
    PDF;

    public static OutputFormat fromParam(String value) {
        if (value == null || value.isBlank()) {
            return null; // auto: imagen→TIFF, PDF→PDF
        }
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "TIFF", "TIF" -> TIFF;
            case "PDF" -> PDF;
            default -> throw new IllegalArgumentException(
                    "outputFormat inválido: use TIFF o PDF"
            );
        };
    }
}
