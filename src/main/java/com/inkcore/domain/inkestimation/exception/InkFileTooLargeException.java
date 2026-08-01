package com.inkcore.domain.inkestimation.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class InkFileTooLargeException extends DomainException {
    public InkFileTooLargeException(String message) {
        super("INK_FILE_TOO_LARGE", message);
    }
}
