package com.indicore.domain.user.exception;

import com.indicore.domain.shared.exception.DomainException;

public class AccountDisabledException extends DomainException {

    public AccountDisabledException() {
        super("ACCOUNT_DISABLED", "La cuenta está desactivada");
    }
}
