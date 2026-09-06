package com.inkcore.domain.order.exception;

import com.inkcore.domain.shared.exception.DomainException;

/**
 * Conflictos de negocio del módulo Pedidos/CxC (HTTP 409).
 */
public class OrderConflictException extends DomainException {

    private static final String CODE = "ORDER_CONFLICT";

    public OrderConflictException(String message) {
        super(CODE, message);
    }
}
