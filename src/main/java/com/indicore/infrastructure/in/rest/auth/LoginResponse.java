package com.indicore.infrastructure.in.rest.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de autenticación (solo emite tokens en este endpoint)")
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
}
