package com.inkcore.domain.paper.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class PaperAlreadyExistsException extends DomainException {

    public PaperAlreadyExistsException(String name) {
        super("CONFLICT", "Ya existe un papel con el mismo nombre, gramaje y formato: " + name);
    }
}
