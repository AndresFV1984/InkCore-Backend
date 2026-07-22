package com.inkcore.domain.user.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class AccountDisabledException extends DomainException {

    public AccountDisabledException() {
        super("ACCOUNT_DISABLED", "La cuenta está desactivada");
    }
}
