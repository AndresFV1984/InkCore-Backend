package com.inkcore.domain.objectstorage.model;

import java.time.Duration;
import java.util.Map;

/**
 * PUT prefirmado para que el navegador suba directo al bucket (sin JWT ni proxy).
 */
public record PresignedUpload(
        String url,
        String method,
        Map<String, String> headers,
        Duration expiresIn
) {
    public PresignedUpload {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url de carga prefirmada es obligatoria");
        }
        method = method == null || method.isBlank() ? "PUT" : method;
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        if (expiresIn == null || expiresIn.isNegative() || expiresIn.isZero()) {
            throw new IllegalArgumentException("expiresIn es obligatorio");
        }
    }
}
