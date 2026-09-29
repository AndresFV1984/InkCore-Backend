package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.machines.MachineResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "MachineSuccessEnvelope", description = "Respuesta exitosa de una máquina")
public record MachineSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-24T12:00:00Z")
        Instant timestamp,
        @Schema(implementation = MachineResponse.class)
        MachineResponse data
) {
}
