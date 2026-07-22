package com.inkcore.application.shared;

import java.time.Instant;
import java.util.List;

/**
 * Puerto de salida: emisión de access token (JWT) y refresh token (opaco).
 */
public interface AccessTokenPort {

    String generateToken(String subject, long tokenVersion, List<String> roles, List<String> permissions);

    String generateRefreshToken();

    long getAccessExpirationSeconds();

    long getRefreshExpirationSeconds();

    /** Instant de expiración del refresh a partir de ahora (UTC). */
    Instant refreshExpiresAt();
}
