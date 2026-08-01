package com.inkcore.domain.colorconversion.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class UnsupportedColorFileException extends DomainException {

    public UnsupportedColorFileException(String detail) {
        super("COLOR_FILE_UNSUPPORTED", detail);
    }
}
