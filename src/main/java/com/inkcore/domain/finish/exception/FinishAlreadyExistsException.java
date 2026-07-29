package com.inkcore.domain.finish.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class FinishAlreadyExistsException extends DomainException {

    private final String field;
    private final String value;

    public FinishAlreadyExistsException(String field, String value) {
        super("CONFLICT", "Finish already exists with " + field + ": " + value);
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
