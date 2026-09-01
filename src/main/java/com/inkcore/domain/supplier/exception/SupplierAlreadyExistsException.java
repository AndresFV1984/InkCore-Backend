package com.inkcore.domain.supplier.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class SupplierAlreadyExistsException extends DomainException {

    private final String field;
    private final String value;

    public SupplierAlreadyExistsException(String field, String value) {
        super("CONFLICT", "Supplier already exists with " + field + ": " + value);
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
