package com.inkcore.infrastructure.in.rest.colorconversions;

import com.inkcore.domain.colorconversion.model.ConversionResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Base64;

@Schema(
        name = "ColorConversionResponse",
        description = "Resultado de la conversión. El archivo CMYK va en fileBase64; el front lo decodifica para descargar. No se guarda en disco."
)
public record ColorConversionResponse(
        @Schema(description = "Nombre del archivo convertido (.tif o .pdf según outputFormat)", example = "arte_CMYK.tif")
        String fileName,

        @Schema(
                description = "MIME del archivo convertido",
                allowableValues = {"image/tiff", "application/pdf"},
                example = "image/tiff"
        )
        String contentType,

        @Schema(
                description = "Contenido del archivo CMYK en Base64 (decodificar en el cliente para descargar)",
                example = "SUkqAAgAAAASAP4ABAABAAAAAAAAAAABBAABAAAAwAkAAAEBBAABAAAA"
        )
        String fileBase64,

        @Schema(description = "Tamaño del archivo de entrada en bytes", example = "1048576")
        long originalSizeBytes,

        @Schema(description = "Tamaño del archivo convertido en bytes", example = "1400000")
        long finalSizeBytes,

        @Schema(description = "Ancho en píxeles", example = "2400")
        int widthPx,

        @Schema(description = "Alto en píxeles", example = "3000")
        int heightPx,

        @Schema(description = "Tiempo de procesamiento en milisegundos", example = "850")
        long processingTimeMs,

        @Schema(
                description = "Intent ICC aplicado",
                allowableValues = {"PERCEPTUAL", "RELATIVE_COLORIMETRIC"},
                example = "PERCEPTUAL"
        )
        String renderingIntent,

        @Schema(description = "Perfil ICC de destino usado", example = "FOGRA39.icc")
        String iccProfile,

        @Schema(description = "brightnessLift efectivo aplicado (0–0.15)", example = "0", minimum = "0", maximum = "0.15")
        float brightnessLift,

        @Schema(description = "vibranceBoost efectivo aplicado (0–0.25)", example = "0", minimum = "0", maximum = "0.25")
        float vibranceBoost,

        @Schema(description = "softProofBrightnessMatch efectivo aplicado", example = "false")
        boolean softProofBrightnessMatch,

        @Schema(description = "true si el aumento de peso es esperado por CMYK", example = "true")
        boolean sizeIncreaseExpected
) {
    public static ColorConversionResponse from(ConversionResult result) {
        return new ColorConversionResponse(
                result.getOutputFileName(),
                result.getOutputMimeType(),
                Base64.getEncoder().encodeToString(result.getConvertedBytes()),
                result.getOriginalSizeBytes(),
                result.getFinalSizeBytes(),
                result.getWidthPx(),
                result.getHeightPx(),
                result.getProcessingTimeMs(),
                result.getRenderingIntent() == null ? null : result.getRenderingIntent().name(),
                result.getIccProfileUsed(),
                result.getBrightnessLift(),
                result.getVibranceBoost(),
                result.isSoftProofBrightnessMatch(),
                true
        );
    }
}
