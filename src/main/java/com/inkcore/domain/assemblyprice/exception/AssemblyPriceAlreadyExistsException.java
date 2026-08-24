package com.inkcore.domain.assemblyprice.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class AssemblyPriceAlreadyExistsException extends DomainException {

    private final String field;
    private final String value;

    public AssemblyPriceAlreadyExistsException(String field, String value) {
        super("CONFLICT", "Assembly price already exists with " + field + ": " + value);
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
