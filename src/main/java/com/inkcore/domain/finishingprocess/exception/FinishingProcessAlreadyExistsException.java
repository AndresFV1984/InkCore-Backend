package com.inkcore.domain.finishingprocess.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class FinishingProcessAlreadyExistsException extends DomainException {

    private final String field;
    private final String value;

    public FinishingProcessAlreadyExistsException(String field, String value) {
        super("CONFLICT", "Finishing process already exists with " + field + ": " + value);
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
