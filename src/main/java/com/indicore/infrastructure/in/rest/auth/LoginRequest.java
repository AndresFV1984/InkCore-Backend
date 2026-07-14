package com.indicore.infrastructure.in.rest.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequest", description = "Credenciales para iniciar sesión")
public record LoginRequest(
        @Schema(description = "Correo electrónico del usuario", example = "admin@indicolors.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El correo es obligatorio")
        String mail,

        @Schema(description = "Contraseña", example = "Indicore2026!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}
