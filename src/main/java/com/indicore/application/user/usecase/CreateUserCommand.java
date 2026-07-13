package com.indicore.application.user.usecase;

import java.util.UUID;

public record CreateUserCommand(
        String companyId,
        String identificationNumber,
        String documentType,
        String name,
        String mail,
        String contact,
        String address,
        String rawPassword,
        UUID roleId,
        String roleCode
) {
}
