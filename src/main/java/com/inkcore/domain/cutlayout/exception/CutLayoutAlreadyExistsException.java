package com.inkcore.domain.cutlayout.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class CutLayoutAlreadyExistsException extends DomainException {

    private final String field;
    private final String value;

    public CutLayoutAlreadyExistsException(String field, String value) {
        super("CONFLICT", "Cut layout already exists with " + field + ": " + value);
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
