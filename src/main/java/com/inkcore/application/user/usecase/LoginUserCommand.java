package com.inkcore.application.user.usecase;

public record LoginUserCommand(
        String mail,
        String rawPassword
) {
}
