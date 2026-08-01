package com.inkcore.domain.colorconversion.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class ConversionIntegrityException extends DomainException {

    public ConversionIntegrityException(String detail) {
        super("COLOR_CONVERSION_INTEGRITY", detail);
    }
}
