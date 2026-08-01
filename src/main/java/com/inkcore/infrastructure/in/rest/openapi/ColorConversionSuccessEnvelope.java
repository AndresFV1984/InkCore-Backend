package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.colorconversions.ColorConversionResponse;
import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "ColorConversionSuccessEnvelope", description = "Respuesta exitosa de conversión de color")
public record ColorConversionSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-07-30T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = ColorConversionResponse.class)
        ColorConversionResponse data
) {
}
