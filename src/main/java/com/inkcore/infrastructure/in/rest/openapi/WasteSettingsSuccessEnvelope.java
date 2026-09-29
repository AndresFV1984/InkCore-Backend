package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.wastesettings.WasteSettingsController;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "WasteSettingsSuccessEnvelope", description = "Respuesta de rangos sugeridos de merma")
public record WasteSettingsSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-24T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = WasteSettingsController.WasteSettingsResponse.class)
        WasteSettingsController.WasteSettingsResponse data
) {
}
