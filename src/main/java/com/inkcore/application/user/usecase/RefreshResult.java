package com.inkcore.application.user.usecase;

/**
 * Resultado de renovación de tokens (access + refresh).
 */
public record RefreshResult(
        String accessToken,
        String refreshToken,
        long tokenAccesExpira,
        long tokenRefresExpira
) {
}
