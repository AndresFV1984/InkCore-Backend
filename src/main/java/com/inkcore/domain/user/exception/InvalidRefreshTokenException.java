package com.inkcore.domain.user.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class InvalidRefreshTokenException extends DomainException {

    public InvalidRefreshTokenException() {
        super(
                "UNAUTHORIZED",
                "Error de autenticación: refresh token inválido, expirado o revocado."
        );
    }
}
