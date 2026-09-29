package com.inkcore.infrastructure.in.rest.openapi;

import com.inkcore.infrastructure.in.rest.envelope.ApiHeaders;
import com.inkcore.infrastructure.in.rest.machines.MachineResponse;
import com.inkcore.infrastructure.in.rest.shared.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "MachineListSuccessEnvelope", description = "Respuesta exitosa del listado paginado de máquinas")
public record MachineListSuccessEnvelope(
        @Schema(description = "Metadatos de la respuesta")
        ApiHeaders headers,
        @Schema(description = "Marca de tiempo UTC", example = "2026-09-24T12:00:00Z")
        Instant timestamp,
        @Schema(description = "Página de máquinas")
        PageResponse<MachineResponse> data
) {
}
