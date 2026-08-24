package com.inkcore.domain.objectstorage.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class InvalidObjectKeyException extends DomainException {

    public InvalidObjectKeyException(String message) {
        super("INVALID_OBJECT_KEY", message);
    }
}
