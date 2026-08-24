package com.inkcore.domain.objectstorage.ports.out;

import com.inkcore.domain.objectstorage.model.PresignedUpload;
import com.inkcore.domain.objectstorage.model.SignedUrl;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * Almacenamiento de objetos (S3 / MinIO). Bucket privado; el navegador usa URLs firmadas.
 */
public interface ObjectStoragePort {

    void putObject(String objectKey, byte[] content, String contentType);

    byte[] getObject(String objectKey);

    /**
     * Tamaño del objeto (Content-Length). {@code 0} si el proveedor no lo declara.
     */
    long objectSize(String objectKey);

    /**
     * Copia el objeto a {@code destination} sin cargarlo entero en heap.
     * Aborta si supera {@code maxBytes}.
     */
    void downloadObject(String objectKey, Path destination, long maxBytes);

    void copyObject(String sourceKey, String destinationKey);

    void deleteObject(String objectKey);

    boolean exists(String objectKey);

    SignedUrl createPresignedGetUrl(String objectKey, Duration ttl);

    /**
     * @param userMetadata claves sin prefijo {@code x-amz-meta-} (p. ej. {@code company-id})
     * @param objectTags   tags S3 (p. ej. ILM de staging). Vacío = sin tags.
     */
    PresignedUpload createPresignedPutUrl(
            String objectKey,
            String contentType,
            Duration ttl,
            Map<String, String> userMetadata,
            Map<String, String> objectTags
    );

    /**
     * Borra staging legado ({@code company/{id}/tmp/...}) más viejo que el umbral configurado.
     * No-op en almacenamiento en memoria.
     */
    default void cleanupLegacyStagingOrphans() {
        // memory / tests
    }
}
