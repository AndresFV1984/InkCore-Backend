package com.inkcore.domain.colorconversion.model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Intent de renderizado ICC.
 * PERCEPTUAL: fotografías (default).
 * RELATIVE_COLORIMETRIC: colores planos / logos.
 */
@Schema(
        name = "RenderingIntent",
        description = "Intent ICC: PERCEPTUAL (fotos) o RELATIVE_COLORIMETRIC (logos/colores planos)"
)
public enum RenderingIntent {
    @Schema(description = "Fotografías y degradados (default)")
    PERCEPTUAL,

    @Schema(description = "Logos y colores planos")
    RELATIVE_COLORIMETRIC;

    public static RenderingIntent fromParam(String value) {
        if (value == null || value.isBlank()) {
            return PERCEPTUAL;
        }
        String normalized = value.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "PERCEPTUAL" -> PERCEPTUAL;
            case "RELATIVE_COLORIMETRIC", "RELATIVE", "RELATIVECOLORIMETRIC" -> RELATIVE_COLORIMETRIC;
            default -> throw new IllegalArgumentException(
                    "renderingIntent inválido: use PERCEPTUAL o RELATIVE_COLORIMETRIC");
        };
    }
}
