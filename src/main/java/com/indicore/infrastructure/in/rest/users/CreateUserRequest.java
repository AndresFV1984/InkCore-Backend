package com.indicore.infrastructure.in.rest.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Size(max = 64)
        String companyId,

        @NotBlank @Size(max = 64)
        String identificationNumber,

        @NotBlank @Size(max = 20)
        String documentType,

        @NotBlank @Size(max = 200)
        String name,

        @NotBlank @Email @Size(max = 320)
        String mail,

        @NotBlank @Size(max = 300)
        String contact,

        @NotBlank @Size(max = 255)
        String address,

        @NotBlank @Size(min = 8, max = 100)
        String password,

        @NotNull
        UUID roleId,

        @NotBlank @Size(max = 50)
        String roleCode
) {
}
