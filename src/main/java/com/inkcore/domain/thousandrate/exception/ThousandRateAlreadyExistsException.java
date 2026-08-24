package com.inkcore.domain.thousandrate.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class ThousandRateAlreadyExistsException extends DomainException {

    private final String field;
    private final String value;

    public ThousandRateAlreadyExistsException(String field, String value) {
        super("CONFLICT", "Thousand rate already exists with " + field + ": " + value);
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
