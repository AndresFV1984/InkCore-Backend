package com.inkcore.domain.colorconversion.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class GhostscriptNotAvailableException extends DomainException {

    public GhostscriptNotAvailableException(String detail) {
        super("GHOSTSCRIPT_NOT_AVAILABLE", detail);
    }
}
