package com.inkcore.domain.papertype.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class PaperTypeAlreadyExistsException extends DomainException {

    private final String field;
    private final String value;

    public PaperTypeAlreadyExistsException(String field, String value) {
        super("CONFLICT", "Paper type already exists with " + field + ": " + value);
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
