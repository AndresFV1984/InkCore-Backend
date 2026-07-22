package com.inkcore.domain.client.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class ClientAlreadyExistsException extends DomainException {

    private final String field;
    private final String value;

    public ClientAlreadyExistsException(String field, String value) {
        super("CONFLICT", "Client already exists with " + field + ": " + value);
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}
