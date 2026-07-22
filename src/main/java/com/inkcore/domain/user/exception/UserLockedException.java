package com.inkcore.domain.user.exception;

import com.inkcore.domain.shared.exception.DomainException;

public class UserLockedException extends DomainException {

    private final long remainingMinutes;

    public UserLockedException(long remainingMinutes) {
        super(
                "ACCOUNT_LOCKED",
                "La cuenta está bloqueada temporalmente. Intente de nuevo en "
                        + remainingMinutes
                        + " minuto(s)"
        );
        this.remainingMinutes = remainingMinutes;
    }

    public long getRemainingMinutes() {
        return remainingMinutes;
    }
}
