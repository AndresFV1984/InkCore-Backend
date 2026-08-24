package com.inkcore.domain.productionorder.exception;

import com.inkcore.domain.shared.exception.DomainException;

import java.util.List;

/**
 * Regla de negocio incumplida en algún paso del wizard de la Orden de Producción.
 */
public class ProductionOrderBusinessRuleException extends DomainException {

    private static final String CODE = "PRODUCTION_ORDER_BUSINESS_RULE";

    private final List<String> errors;

    public ProductionOrderBusinessRuleException(String message) {
        super(CODE, message);
        this.errors = List.of();
    }

    public ProductionOrderBusinessRuleException(String message, List<String> errors) {
        super(CODE, message);
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
