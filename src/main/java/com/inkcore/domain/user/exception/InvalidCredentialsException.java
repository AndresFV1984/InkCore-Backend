package com.inkcore.domain.user.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("UNAUTHORIZED", "Credenciales inválidas");
    }
}
