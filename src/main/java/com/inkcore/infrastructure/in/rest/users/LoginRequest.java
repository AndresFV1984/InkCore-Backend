package com.inkcore.infrastructure.in.rest.users;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "LoginRequest", description = "Credenciales para iniciar sesión")
public record LoginRequest(
        @Schema(description = "Correo electrónico del usuario", example = "usuario@empresa.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        @Size(max = 320, message = "El correo no puede superar 320 caracteres")
        String mail,

        @Schema(description = "Contraseña", example = "SecurePass123*", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 200, message = "La contraseña debe tener entre 8 y 200 caracteres")
        String password
) {
}
