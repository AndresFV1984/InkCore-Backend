package com.inkcore.application.user.usecase;

import java.util.List;

public record UpdateUserCommand(
        String userId,
        String identificationNumber,
        String documentType,
        String name,
        String mail,
        String contact,
        String department,
        String city,
        String address,
        String rawPassword,
        String roleCode,
        List<String> permissionCodes,
        boolean state
) {
}
