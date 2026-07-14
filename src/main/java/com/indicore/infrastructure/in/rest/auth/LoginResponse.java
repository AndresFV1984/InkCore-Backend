package com.indicore.infrastructure.in.rest.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginResponse", description = "Tokens emitidos tras autenticación correcta")
public record LoginResponse(
        @Schema(
                description = "Access token JWT (HS256). Claims: sub (userId), tv (token_version), roles",
                example = "eyJhbGciOiJIUzI1NiJ9..."
        )
        String accessToken,

        @Schema(description = "Tipo de token", example = "Bearer")
        String tokenType,

        @Schema(description = "Segundos hasta la expiración del access token", example = "3600")
        long expiresInSeconds
) {
}
