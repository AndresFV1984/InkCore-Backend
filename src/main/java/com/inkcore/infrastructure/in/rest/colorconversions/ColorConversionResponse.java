package com.inkcore.infrastructure.in.rest.colorconversions;

import com.inkcore.domain.colorconversion.model.ConversionResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Base64;

@Schema(
        name = "ColorConversionResponse",
        description = """
                Resultado de la conversión (CMM LittleCMS + ICC).
                - previewRgbBase64 + previewContentType: JPEG para UI (soft-proof; con softProof=true
                  alineado a referencia de pantalla). No comparar con visores CMYK sin ICC.
                - fileBase64 + contentType + fileName: TIFF/PDF CMYK para descarga/CTP
                  (incluye lift/vibrance/apertura CMY si softProof=true; FIDELITY = ICC puro).
                - softProofLumaRatio: métrica de paridad luma (null en PDF o FIDELITY sin soft-proof).
                No se guarda en disco.
                """
)
public record ColorConversionResponse(
        @Schema(description = "Nombre del archivo convertido (.tif o .pdf según outputFormat)", example = "foto_CMYK.tif")
        String fileName,

        @Schema(
                description = "MIME del archivo CMYK (descarga/CTP). No usar para preview en navegador.",
                allowableValues = {"image/tiff", "application/pdf"},
                example = "image/tiff"
        )
        String contentType,

        @Schema(
                description = "Archivo CMYK en Base64 para descarga/CTP (no para <img>)",
                example = "SUkqAAgAAAASAP4ABAABAAAAAAAAAAABBAABAAAAwAkAAAEBBAABAAAA"
        )
        String fileBase64,

        @Schema(description = "Tamaño del archivo de entrada en bytes", example = "1048576")
        long originalSizeBytes,

        @Schema(description = "Tamaño del archivo convertido en bytes", example = "1400000")
        long finalSizeBytes,

        @Schema(description = "Ancho en píxeles (igual a la entrada)", example = "2400")
        int widthPx,

        @Schema(description = "Alto en píxeles (igual a la entrada)", example = "3000")
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

        @Schema(description = "brightnessLift efectivo aplicado (0–0.20)", example = "0.12", minimum = "0", maximum = "0.20")
        float brightnessLift,

        @Schema(description = "vibranceBoost efectivo aplicado (0–0.35)", example = "0.28", minimum = "0", maximum = "0.35")
        float vibranceBoost,

        @Schema(description = """
                softProofBrightnessMatch efectivo. Si true: lift/vibrance + apertura CMY en el CMYK
                y preview UI alineado a referencia de pantalla.
                """, example = "true")
        boolean softProofBrightnessMatch,

        @Schema(
                description = "Preset solicitado en el request (null si no se envió)",
                allowableValues = {"FIDELITY", "COMMERCIAL", "VIVID"},
                example = "COMMERCIAL",
                nullable = true
        )
        String qualityPreset,

        @Schema(
                description = """
                        Soft-proof RGB JPEG en Base64 a resolución nativa para <img>.
                        Con softProof=true (COMMERCIAL/VIVID) incluye alineación UI hacia el RGB
                        de referencia; no es el archivo CTP. Null en salidas PDF.
                        """,
                example = "/9j/4AAQSkZJRgABAQAAAQABAAD..."
        )
        String previewRgbBase64,

        @Schema(
                description = "MIME del preview para UI",
                allowableValues = {"image/jpeg"},
                example = "image/jpeg",
                nullable = true
        )
        String previewContentType,

        @Schema(
                description = """
                        Relación luma(soft-proof CMYK→RGB) / luma(RGB original) sobre el contenido.
                        Ideal ~0.95–1.05 cuando softProofBrightnessMatch=true. Null en PDF o si no se midió.
                        """,
                example = "0.97",
                nullable = true
        )
        Double softProofLumaRatio,

        @Schema(description = "true si el aumento de peso es esperado por CMYK (4 canales)", example = "true")
        boolean sizeIncreaseExpected
) {
    public static ColorConversionResponse from(ConversionResult result) {
        byte[] preview = result.getPreviewRgbBytes();
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
                result.getQualityPreset() == null ? null : result.getQualityPreset().name(),
                preview == null || preview.length == 0 ? null : Base64.getEncoder().encodeToString(preview),
                result.getPreviewMimeType(),
                result.getSoftProofLumaRatio(),
                true
        );
    }
}
