package com.inkcore.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.password")
public class PasswordPolicyProperties {

    private int maxFailedAttempts = 5;
    private int lockDurationMinutes = 15;
    private int expirationDays = 90;
    private int warningDays = 7;

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public void setMaxFailedAttempts(int maxFailedAttempts) {
        this.maxFailedAttempts = maxFailedAttempts;
    }

    public int getLockDurationMinutes() {
        return lockDurationMinutes;
    }

    public void setLockDurationMinutes(int lockDurationMinutes) {
        this.lockDurationMinutes = lockDurationMinutes;
    }

    public int getExpirationDays() {
        return expirationDays;
    }

    public void setExpirationDays(int expirationDays) {
        this.expirationDays = expirationDays;
    }

    public int getWarningDays() {
        return warningDays;
    }

    public void setWarningDays(int warningDays) {
        this.warningDays = warningDays;
    }
}
