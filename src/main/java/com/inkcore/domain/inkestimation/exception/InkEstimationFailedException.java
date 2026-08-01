package com.inkcore.domain.inkestimation.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class InkEstimationFailedException extends DomainException {
    public InkEstimationFailedException(String message) {
        super("INK_ESTIMATION_FAILED", message);
    }

    public InkEstimationFailedException(String message, Throwable cause) {
        super("INK_ESTIMATION_FAILED", message);
        initCause(cause);
    }
}
