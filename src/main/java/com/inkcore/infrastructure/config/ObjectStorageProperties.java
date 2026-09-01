package com.inkcore.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "inkcore.object-storage")
public class ObjectStorageProperties {

    public static final String INSECURE_DEFAULT_SECRET = "inkcore-app-dev-only-change-me";

    /** s3 | memory (tests/local sin MinIO) */
    private String type = "s3";

    /** Endpoint interno (la app: {@code http://minio:9000} en Compose). */
    private String endpoint = "";

    /**
     * Host que ve el navegador en URLs prefirmadas.
     * En Docker: {@code http://localhost:9100}. Vacío = mismo que {@code endpoint}.
     */
    private String publicEndpoint = "";

    private String region = "us-east-1";

    private String bucket = "inkcore";

    /**
     * Access key del usuario de aplicación (no el root de MinIO).
     * Política: List/Get/Put/Delete (+ tags) sobre el bucket. CORS e ILM los aplica el init con root.
     * El aislamiento entre empresas sigue en JWT. Rotación: recrear el usuario y reiniciar la app.
     */
    private String accessKey = "inkcore-app";

    private String secretKey = INSECURE_DEFAULT_SECRET;

    private boolean pathStyleAccess = true;

    /** TTL URL firmada (segundos). Rango recomendado 60–900. */
    private int presignedUrlTtlSeconds = 300;

    /** Máximo arte original (alineado con estimación multipart / inkcore.ink-estimation.absolute-max-file-bytes). */
    private long maxAssetFileBytes = 512L * 1024L * 1024L;

    /** Máximo miniatura JPEG (~2 MB). */
    private long maxPreviewFileBytes = 2_097_152L;

    /** Orígenes CORS del bucket (CSV) para PUT/GET desde el front. */
    private String corsAllowedOrigins = "http://localhost:8085,http://127.0.0.1:8085";

    /**
     * Días hasta borrar objetos de staging con tag {@code inkcore-staging=true}.
     * {@code 0} desactiva esa regla. Recomendado 1–2.
     */
    private int stagingExpirationDays = 7;

    /**
     * Respaldo ILM por prefijo {@code tmp/} (no depende de tags; S3 no soporta glob).
     * {@code 0} desactiva. Debe ser mayor que {@code staging-expiration-days}.
     */
    private int stagingFallbackExpirationDays = 30;

    /**
     * Días para borrar huérfanos del path legado {@code company/{id}/tmp/...}
     * (no los cubre el ILM de {@code tmp/}). {@code 0} desactiva el barrido.
     */
    private int legacyStagingCleanupDays = 7;

    /**
     * Intervalo del job periódico de barrido legado (ms). Default 24 h.
     * El barrido no es solo boot-time: un front viejo en caché puede seguir subiendo al path viejo.
     */
    private long legacyStagingCleanupIntervalMs = 86_400_000L;

    /**
     * Si true, la app crea el bucket y aplica CORS/ILM (MinIO local sin minio-init).
     * En Compose/prod el init lo hace con root; la app no necesita esos privilegios.
     */
    private boolean manageBucket = false;

    /**
     * Permite el secreto de ejemplo {@code inkcore-app-dev-only-change-me}. Solo local/docker/test.
     * Prod debe dejar esto en false y definir OBJECT_STORAGE_SECRET_KEY real.
     */
    private boolean allowInsecureDefaults = false;

    /** Reintentos al primer contacto con S3/MinIO (usuario aún no visible, red, etc.). */
    private int startupRetryAttempts = 12;

    /** Espera entre reintentos de arranque (ms). */
    private long startupRetryBackoffMs = 2000L;

    /**
     * Reintentos de Get/Copy/Head en runtime (timeouts, 503, bucket ausente).
     * El SDK ya reintenta HTTP; esto cubre el stream cortado a mitad del GET y el copy que expiró.
     */
    private int operationRetryAttempts = 3;

    /** Espera entre reintentos de operación (ms). */
    private long operationRetryBackoffMs = 200L;

    /**
     * Timeout de un intento HTTP a MinIO/S3 (ms). {@code 0} = sin límite en el cliente.
     * El GET de un arte de ~25 MB debe caber aquí.
     */
    private long operationTimeoutMs = 60_000L;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getPublicEndpoint() {
        return publicEndpoint;
    }

    public void setPublicEndpoint(String publicEndpoint) {
        this.publicEndpoint = publicEndpoint;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isPathStyleAccess() {
        return pathStyleAccess;
    }

    public void setPathStyleAccess(boolean pathStyleAccess) {
        this.pathStyleAccess = pathStyleAccess;
    }

    public int getPresignedUrlTtlSeconds() {
        return presignedUrlTtlSeconds;
    }

    public void setPresignedUrlTtlSeconds(int presignedUrlTtlSeconds) {
        this.presignedUrlTtlSeconds = presignedUrlTtlSeconds;
    }

    public long getMaxAssetFileBytes() {
        return maxAssetFileBytes;
    }

    public void setMaxAssetFileBytes(long maxAssetFileBytes) {
        this.maxAssetFileBytes = maxAssetFileBytes;
    }

    public long getMaxPreviewFileBytes() {
        return maxPreviewFileBytes;
    }

    public void setMaxPreviewFileBytes(long maxPreviewFileBytes) {
        this.maxPreviewFileBytes = maxPreviewFileBytes;
    }

    public String getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(String corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public int getStagingExpirationDays() {
        return stagingExpirationDays;
    }

    public void setStagingExpirationDays(int stagingExpirationDays) {
        this.stagingExpirationDays = stagingExpirationDays;
    }

    public int getStagingFallbackExpirationDays() {
        return stagingFallbackExpirationDays;
    }

    public void setStagingFallbackExpirationDays(int stagingFallbackExpirationDays) {
        this.stagingFallbackExpirationDays = stagingFallbackExpirationDays;
    }

    public int getLegacyStagingCleanupDays() {
        return legacyStagingCleanupDays;
    }

    public void setLegacyStagingCleanupDays(int legacyStagingCleanupDays) {
        this.legacyStagingCleanupDays = legacyStagingCleanupDays;
    }

    public long getLegacyStagingCleanupIntervalMs() {
        return legacyStagingCleanupIntervalMs;
    }

    public void setLegacyStagingCleanupIntervalMs(long legacyStagingCleanupIntervalMs) {
        this.legacyStagingCleanupIntervalMs = legacyStagingCleanupIntervalMs;
    }

    public boolean isManageBucket() {
        return manageBucket;
    }

    public void setManageBucket(boolean manageBucket) {
        this.manageBucket = manageBucket;
    }

    public boolean isAllowInsecureDefaults() {
        return allowInsecureDefaults;
    }

    public void setAllowInsecureDefaults(boolean allowInsecureDefaults) {
        this.allowInsecureDefaults = allowInsecureDefaults;
    }

    public int getStartupRetryAttempts() {
        return startupRetryAttempts;
    }

    public void setStartupRetryAttempts(int startupRetryAttempts) {
        this.startupRetryAttempts = startupRetryAttempts;
    }

    public long getStartupRetryBackoffMs() {
        return startupRetryBackoffMs;
    }

    public void setStartupRetryBackoffMs(long startupRetryBackoffMs) {
        this.startupRetryBackoffMs = startupRetryBackoffMs;
    }

    public int getOperationRetryAttempts() {
        return operationRetryAttempts;
    }

    public void setOperationRetryAttempts(int operationRetryAttempts) {
        this.operationRetryAttempts = operationRetryAttempts;
    }

    public long getOperationRetryBackoffMs() {
        return operationRetryBackoffMs;
    }

    public void setOperationRetryBackoffMs(long operationRetryBackoffMs) {
        this.operationRetryBackoffMs = operationRetryBackoffMs;
    }

    public long getOperationTimeoutMs() {
        return operationTimeoutMs;
    }

    public void setOperationTimeoutMs(long operationTimeoutMs) {
        this.operationTimeoutMs = operationTimeoutMs;
    }
}
