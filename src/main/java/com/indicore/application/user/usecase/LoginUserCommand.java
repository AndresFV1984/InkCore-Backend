package com.indicore.application.user.usecase;

public record LoginUserCommand(
        String mail,
        String rawPassword
) {
}
