package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.colorconversions.ColorConversionResponse;
import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ColorConversionSuccessEnvelope", description = """
        Respuesta exitosa de POST /api/v1/color-conversions/convert.
        data.previewRgbBase64 = JPEG UI (soft-proof; no usar TIFF/PDF en el navegador).
        data.fileBase64 = archivo CMYK TIFF/PDF para descarga/CTP.
        Presets: FIDELITY | COMMERCIAL | VIVID. Defaults comerciales:
        brightnessLift=0.12, vibranceBoost=0.28, softProofBrightnessMatch=true.
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
