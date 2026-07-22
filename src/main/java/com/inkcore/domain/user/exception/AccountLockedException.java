package com.inkcore.domain.user.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class AccountLockedException extends DomainException {

    public AccountLockedException() {
        super("ACCOUNT_LOCKED", "La cuenta está bloqueada temporalmente");
    }
}
