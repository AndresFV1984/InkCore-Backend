package com.inkcore.domain.order.exception;

import com.inkcore.domain.shared.exception.DomainException;

/**
 * El trigger de entregas rechazó el movimiento porque planta aún no ha
 * liberado suficientes unidades.
 */
public class InsufficientAvailabilityException extends DomainException {

    private static final String CODE = "INSUFFICIENT_AVAILABILITY";

    public InsufficientAvailabilityException(String message) {
        super(CODE, message);
    }
}
