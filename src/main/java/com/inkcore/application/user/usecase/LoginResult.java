package com.inkcore.application.user.usecase;

import com.inkcore.domain.user.model.User;

import java.util.List;

public record LoginResult(
        String accessToken,
        String refreshToken,
        long accessExpiresInSeconds,
        long refreshExpiresInSeconds,
        User user,
        PasswordExpirationWarning passwordExpirationWarning,
        List<RolePermissions> roles
) {
    public record PasswordExpirationWarning(boolean showWarning, long daysUntilExpiration) {
    }

    public record RolePermissions(String role, List<String> permissions) {
    }
}
