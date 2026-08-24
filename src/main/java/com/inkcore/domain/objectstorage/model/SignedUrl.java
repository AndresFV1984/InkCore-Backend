package com.inkcore.domain.objectstorage.model;

import java.time.Duration;

/**
 * URL pre-firmada de lectura sobre un objeto privado.
 */
public record SignedUrl(String url, Duration expiresIn) {
}
