package com.inkcore.infrastructure.out.objectstorage;

import com.inkcore.domain.objectstorage.model.PresignedUpload;
import com.inkcore.domain.objectstorage.model.SignedUrl;
import com.inkcore.domain.objectstorage.ports.out.ObjectStoragePort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Almacenamiento en memoria para tests y perfil memory.
 */
public class InMemoryObjectStorageAdapter implements ObjectStoragePort {

    private final Map<String, StoredObject> objects = new ConcurrentHashMap<>();

    @Override
    public void putObject(String objectKey, byte[] content, String contentType) {
        objects.put(objectKey, new StoredObject(content, contentType));
    }

    @Override
    public byte[] getObject(String objectKey) {
        StoredObject stored = require(objectKey);
        return stored.content();
    }

    @Override
    public long objectSize(String objectKey) {
        return require(objectKey).content().length;
    }

    @Override
    public void downloadObject(String objectKey, Path destination, long maxBytes) {
        byte[] content = require(objectKey).content();
        if (content.length > maxBytes) {
            throw new IllegalArgumentException("Objeto supera el tamaño máximo permitido");
        }
        try {
            Files.write(destination, content);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo escribir el objeto " + objectKey, ex);
        }
    }

    @Override
    public void copyObject(String sourceKey, String destinationKey) {
        StoredObject source = require(sourceKey);
        objects.put(destinationKey, new StoredObject(source.content(), source.contentType()));
    }

    @Override
    public void deleteObject(String objectKey) {
        objects.remove(objectKey);
    }

    @Override
    public boolean exists(String objectKey) {
        return objects.containsKey(objectKey);
    }

    @Override
    public SignedUrl createPresignedGetUrl(String objectKey, Duration ttl) {
        if (!exists(objectKey)) {
            throw new IllegalArgumentException("Objeto no encontrado: " + objectKey);
        }
        return new SignedUrl("memory://" + objectKey, ttl);
    }

    @Override
    public PresignedUpload createPresignedPutUrl(
            String objectKey,
            String contentType,
            Duration ttl,
            Map<String, String> userMetadata,
            Map<String, String> objectTags
    ) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (contentType != null && !contentType.isBlank()) {
            headers.put("Content-Type", contentType);
        }
        if (userMetadata != null) {
            userMetadata.forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                    headers.put("x-amz-meta-" + key, value);
                }
            });
        }
        if (objectTags != null && !objectTags.isEmpty()) {
            String tagging = objectTags.entrySet().stream()
                    .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank()
                            && entry.getValue() != null && !entry.getValue().isBlank())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .reduce((a, b) -> a + "&" + b)
                    .orElse("");
            if (!tagging.isEmpty()) {
                headers.put("x-amz-tagging", tagging);
            }
        }
        return new PresignedUpload("memory://put/" + objectKey, "PUT", headers, ttl);
    }

    private StoredObject require(String objectKey) {
        StoredObject stored = objects.get(objectKey);
        if (stored == null) {
            throw new IllegalArgumentException("Objeto no encontrado: " + objectKey);
        }
        return stored;
    }

    private record StoredObject(byte[] content, String contentType) {
    }
}
