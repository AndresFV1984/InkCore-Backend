package com.inkcore.infrastructure.in.rest.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "RefreshTokenRequest", description = "Solicitud de renovación de access token")
public record RefreshTokenRequest(
        @Schema(
                description = "Refresh token opaco emitido en login o refresh anterior",
                example = "dGhpc2lzYXJlZnJlc2h0b2tlbm9wYXF1ZQ",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "refreshToken is required")
        String refreshToken
) {
}
