package com.inkcore.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * CORS whitelist. Nunca usar {@code *}.
 * <p>
 * Lista en YAML ({@code app.cors.allowed-origins}) y/o CSV vía env
 * {@code CORS_ALLOWED_ORIGINS} / {@code app.cors.origins} (se unen).
 */
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /** Orígenes permitidos (preferido en YAML). */
    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * CSV adicional (staging/prod), p. ej. {@code CORS_ALLOWED_ORIGINS=https://app.example.com}.
     * Se combina con {@link #allowedOrigins}.
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
            "X-Requested-With",
            "X-Correlation-Id"
    );

    private List<String> exposedHeaders = List.of(
            "Authorization",
            "X-Correlation-Id"
    );

    /**
     * false: JWT en header Authorization (sin cookies cross-origin).
     * true solo con orígenes concretos (nunca *).
     */
    private boolean allowCredentials = false;

    private long maxAgeSeconds = 3600;

    public List<String> resolveAllowedOrigins() {
        Set<String> resolved = new LinkedHashSet<>();
        if (allowedOrigins != null) {
            for (String origin : allowedOrigins) {
                if (origin != null && !origin.isBlank()) {
                    resolved.add(origin.trim());
                }
            }
        }
        if (origins != null && !origins.isBlank()) {
            Arrays.stream(origins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(resolved::add);
        }
        return List.copyOf(resolved);
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
