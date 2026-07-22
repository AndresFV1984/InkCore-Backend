package com.inkcore.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CORS whitelist. Nunca usar {@code *}.
 * <p>
 * Lista en YAML ({@code app.cors.allowed-origins}) o CSV vía env
 * {@code CORS_ALLOWED_ORIGINS} / {@code app.cors.origins}.
 */
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /** Orígenes permitidos (preferido en YAML). */
    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * Alternativa CSV (staging/prod), p. ej. {@code CORS_ALLOWED_ORIGINS=https://app.example.com}.
     * Se usa si {@link #allowedOrigins} está vacío.
     */
    private String origins = "";

    private List<String> allowedMethods = List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    );

    private List<String> allowedHeaders = List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Correlation-Id"
    );

    private List<String> exposedHeaders = List.of(
            "X-Correlation-Id"
    );

    /**
     * false: JWT en header Authorization (sin cookies cross-origin).
     * true solo con orígenes concretos (nunca *).
     */
    private boolean allowCredentials = false;

    private long maxAgeSeconds = 3600;

    public List<String> resolveAllowedOrigins() {
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            return List.copyOf(allowedOrigins);
        }
        if (origins == null || origins.isBlank()) {
            return List.of();
        }
        return Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins != null ? allowedOrigins : new ArrayList<>();
    }

    public String getOrigins() {
        return origins;
    }

    public void setOrigins(String origins) {
        this.origins = origins != null ? origins : "";
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods != null ? allowedMethods : List.of();
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders != null ? allowedHeaders : List.of();
    }

    public List<String> getExposedHeaders() {
        return exposedHeaders;
    }

    public void setExposedHeaders(List<String> exposedHeaders) {
        this.exposedHeaders = exposedHeaders != null ? exposedHeaders : List.of();
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public long getMaxAgeSeconds() {
        return maxAgeSeconds;
    }

    public void setMaxAgeSeconds(long maxAgeSeconds) {
        this.maxAgeSeconds = maxAgeSeconds;
    }
}
