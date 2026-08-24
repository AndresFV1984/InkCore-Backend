package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.colorconversions.IccProfileResponse;
import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(
        name = "ColorConversionIccProfileListSuccessEnvelope",
        description = "Respuesta exitosa de GET /api/v1/color-conversions/list (catálogo ICC de destino)"
)
public record ColorConversionIccProfileListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-08-04T12:00:00Z")
        Instant timestamp,
        @ArraySchema(
                arraySchema = @Schema(description = "Perfiles ICC de destino CMYK"),
                schema = @Schema(implementation = IccProfileResponse.class)
        )
        List<IccProfileResponse> data
) {
}
