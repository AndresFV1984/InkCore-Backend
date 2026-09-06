package com.inkcore.domain.order.exception;

import com.inkcore.domain.shared.exception.DomainException;

import java.util.List;

public class OrderBusinessRuleException extends DomainException {

    private static final String CODE = "ORDER_BUSINESS_RULE";

    private final List<String> errors;

    public OrderBusinessRuleException(String message) {
        super(CODE, message);
        this.errors = List.of();
    }

    public OrderBusinessRuleException(String message, List<String> errors) {
        super(CODE, message);
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
