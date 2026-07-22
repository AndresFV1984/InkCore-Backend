package com.inkcore.application.shared;

import java.time.Instant;

/**
 * Registro stateful de un refresh token opaco.
 */
public record RefreshTokenRecord(
        String refreshToken,
        String userId,
        long tokenVersion,
        Instant expiresAt
) {
}
