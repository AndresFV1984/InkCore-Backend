package com.inkcore.domain.user.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class UserNotFoundException extends DomainException {

    public UserNotFoundException(String userId) {
        super("NOT_FOUND", "Usuario no encontrado: " + userId);
    }
}
