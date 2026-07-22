package com.inkcore.infrastructure.in.rest.users;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(
        name = "CreateUserRequest",
        description = """
                Alta de usuario (POST /api/v1/users).
                Enviar `role` (código o nombre). Alias opcionales: `roleName`.
                Campos de seguridad (tokenVersion, lock, etc.) los asigna el servidor.
                """
)
public record CreateUserRequest(
        @Schema(description = "Identificador de empresa", example = "company-seed-001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "La empresa es obligatoria")
        @Size(max = 64)
        String companyId,

        @Schema(description = "Número de identificación", example = "1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 64)
        String identificationNumber,

        @Schema(
                description = "Tipo de documento",
                example = "CC",
                allowableValues = {"CC", "CE", "TI", "PA", "NIT"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Size(max = 20)
        String documentType,

        @Schema(description = "Nombre completo", example = "Ana García", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 200)
        String name,

        @Schema(description = "Correo electrónico", example = "ana@itm.edu.co", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Email
        @Size(max = 320)
        String mail,

        @Schema(description = "Teléfono / contacto", example = "3001234567", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 300)
        String contact,

        @Schema(description = "Departamento", example = "Antioquia", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        String department,

        @Schema(description = "Ciudad / municipio", example = "Medellín", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 100)
        String city,

        @Schema(description = "Dirección (opcional)", example = "Calle 10 # 20-30")
        @Size(max = 255)
        String address,

        @Schema(
                description = "Contraseña en texto plano (se guarda como password_hash). Mín. 8; mayúscula, minúscula, número y especial.",
                example = "SecurePass123*",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Size(min = 8, max = 200)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{8,}$",
                message = "La contraseña debe incluir mayúscula, minúscula, número y carácter especial"
        )
        String password,

        @Schema(
                description = "Rol (código o nombre). Crea user_roles y toma permisos del rol en BD. Ej: ADMINISTRADOR o Administrador",
                example = "ADMINISTRADOR",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Size(max = 100)
        String role,

        @Schema(
                description = "Alias opcional de `role` (compat)",
                example = "Administrador",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Size(max = 100)
        String roleName,

        @Schema(description = "Estado: true=activo, false=inactivo", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Boolean state
) {
    @JsonIgnore
    @Schema(hidden = true)
    public String resolvedRole() {
        if (role != null && !role.isBlank()) {
            return role.trim();
        }
        if (roleName != null && !roleName.isBlank()) {
            return roleName.trim();
        }
        return null;
    }

    @AssertTrue(message = "El rol es obligatorio (enviar role o roleName)")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isRolePresent() {
        return resolvedRole() != null;
    }
}
