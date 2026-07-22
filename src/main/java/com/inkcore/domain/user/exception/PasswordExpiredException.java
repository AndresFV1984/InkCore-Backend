package com.inkcore.domain.user.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class PasswordExpiredException extends DomainException {

    public PasswordExpiredException() {
        super("PASSWORD_EXPIRED", "La contraseña ha expirado; debe cambiarla antes de iniciar sesión");
    }
}
