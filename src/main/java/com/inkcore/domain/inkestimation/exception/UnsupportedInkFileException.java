package com.inkcore.domain.inkestimation.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class UnsupportedInkFileException extends DomainException {
    public UnsupportedInkFileException(String message) {
        super("UNSUPPORTED_INK_FILE", message);
    }
}
