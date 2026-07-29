package com.inkcore.domain.bankaccount.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class BankAccountAlreadyExistsException extends DomainException {

    private final String field;
    private final String value;

    public BankAccountAlreadyExistsException(String field, String value) {
        super("CONFLICT", "Bank account already exists with " + field + ": " + value);
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
