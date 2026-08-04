package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.colorconversions.ColorConversionResponse;
import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ColorConversionSuccessEnvelope", description = """
        Respuesta exitosa de conversión de color.
        data.previewRgbBase64 = preview UI (JPEG soft-proof; usar en <img>).
        data.fileBase64 = archivo CMYK TIFF/PDF para descarga/CTP.
        Defaults comerciales recomendados en request: brightnessLift=0.12, vibranceBoost=0.28,
        softProofBrightnessMatch=true (o qualityPreset=COMMERCIAL).
        """)
public record ColorConversionSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-07-30T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = ColorConversionResponse.class)
        ColorConversionResponse data
) {
}
