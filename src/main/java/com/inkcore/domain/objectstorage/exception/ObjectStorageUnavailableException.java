package com.inkcore.domain.objectstorage.exception;

import com.inkcore.domain.shared.exception.DomainException;

/**
 * MinIO/S3 no respondió a tiempo o el bucket no estuvo disponible de forma transitoria.
 * Tras reintentos, el API responde 503 para que el cliente reintente.
 */
public class ObjectStorageUnavailableException extends DomainException {

    public ObjectStorageUnavailableException(String message) {
        super("OBJECT_STORAGE_UNAVAILABLE", message);
    }

    public ObjectStorageUnavailableException(String message, Throwable cause) {
        super("OBJECT_STORAGE_UNAVAILABLE", message, cause);
    }
}
