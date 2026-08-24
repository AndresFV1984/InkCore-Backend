package com.inkcore.domain.objectstorage.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class ObjectStorageAccessDeniedException extends DomainException {

    public ObjectStorageAccessDeniedException(String message) {
        super("OBJECT_STORAGE_ACCESS_DENIED", message);
    }
}
