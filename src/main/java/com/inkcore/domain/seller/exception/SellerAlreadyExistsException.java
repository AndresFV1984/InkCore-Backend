package com.inkcore.domain.seller.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class SellerAlreadyExistsException extends DomainException {

    private final String field;
    private final String value;

    public SellerAlreadyExistsException(String field, String value) {
        super("CONFLICT", "Seller already exists with " + field + ": " + value);
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
