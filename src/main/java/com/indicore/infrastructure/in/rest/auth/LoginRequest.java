package com.indicore.infrastructure.in.rest.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El correo es obligatorio")
        String mail,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}
