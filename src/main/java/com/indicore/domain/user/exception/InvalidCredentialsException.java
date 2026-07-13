package com.indicore.domain.user.exception;

import com.indicore.domain.shared.exception.DomainException;

public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "Credenciales inválidas");
    }
}
