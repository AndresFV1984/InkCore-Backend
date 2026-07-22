package com.inkcore.domain.user.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class UserAlreadyExistsException extends DomainException {

    private final String field;
    private final String value;

    public UserAlreadyExistsException(String field, String value) {
        super("CONFLICT", "User already exists with " + field + ": " + value);
        this.field = field;
        this.value = value;
    }

    /** Compatibilidad con mensajes libres previos. */
    public UserAlreadyExistsException(String message) {
        super("CONFLICT", message);
        this.field = null;
        this.value = null;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}
