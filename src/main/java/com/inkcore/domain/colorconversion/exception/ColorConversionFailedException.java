package com.inkcore.domain.colorconversion.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class ColorConversionFailedException extends DomainException {

    public ColorConversionFailedException(String detail) {
        super("COLOR_CONVERSION_FAILED", detail);
    }

    public ColorConversionFailedException(String detail, Throwable cause) {
        super("COLOR_CONVERSION_FAILED", detail);
        initCause(cause);
    }
}
