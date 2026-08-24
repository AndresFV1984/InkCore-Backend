package com.inkcore.domain.platetype.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class PlateTypeAlreadyExistsException extends DomainException {

    private final String field;
    private final String value;

    public PlateTypeAlreadyExistsException(String field, String value) {
        super("CONFLICT", "Plate type already exists with " + field + ": " + value);
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
