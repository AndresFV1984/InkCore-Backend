package com.indicore.application.user.usecase;

public record LoginResult(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
}
