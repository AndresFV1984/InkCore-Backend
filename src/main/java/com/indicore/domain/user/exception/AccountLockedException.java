package com.indicore.domain.user.exception;

import com.indicore.domain.shared.exception.DomainException;

public class AccountLockedException extends DomainException {

    public AccountLockedException() {
        super("ACCOUNT_LOCKED", "La cuenta está bloqueada temporalmente");
    }
}
