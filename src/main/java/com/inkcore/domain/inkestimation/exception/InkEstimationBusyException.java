package com.inkcore.domain.inkestimation.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class InkEstimationBusyException extends DomainException {

    public InkEstimationBusyException(String message) {
        super("INK_ESTIMATION_BUSY", message);
    }
}
