package com.inkcore.infrastructure.in.rest.users;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(
        name = "RegisterUserRequest",
        description = """
                Alta vía POST /api/v1/users/register.
                Enviar `role` (código o nombre). Alias: `roleCode` / `roleName`.
                Crea la relación en user_roles; los permisos se toman del rol en BD.
                """
)
public record RegisterUserRequest(
        @Schema(description = "Identificador de empresa (opcional)", example = "company-seed-001", maxLength = 64)
        @Size(max = 64)
        String companyId,

        @Schema(description = "Nombre completo", example = "María Fernanda López", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 200)
        String name,

        @Schema(description = "Tipo de documento", example = "CC", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 20)
        String documentType,

        @Schema(description = "Número de identificación", example = "1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 64)
        String identificationNumber,

        @Schema(description = "Correo electrónico", example = "usuario@empresa.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email @Size(max = 320)
        String mail,

        @Schema(description = "Teléfono / contacto (opcional)", example = "300 123 4567")
        @Size(max = 300)
        String contact,

        @Schema(description = "Departamento", example = "Antioquia", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 100)
        String department,

        @Schema(description = "Ciudad / municipio", example = "Medellín", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 100)
        String city,

        @Schema(description = "Dirección (opcional)", example = "Calle 10 # 20-30")
        @Size(max = 255)
        String address,

        @Schema(
                description = "Rol (código o nombre). Preferido. Ej: ADMINISTRADOR",
                example = "ADMINISTRADOR",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Size(max = 100)
        String role,

        @Schema(
                description = "Alias opcional de `role`",
                example = "ADMINISTRADOR",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Size(max = 100)
        String roleCode,

        @Schema(
                description = "Alias opcional de `role`",
                example = "Administrador",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Size(max = 100)
        String roleName,

        @ArraySchema(arraySchema = @Schema(description = "Códigos de permisos (opcional; si se omite se usan los del rol)"), schema = @Schema(example = "production.orders.view"))
        List<@NotBlank @Size(max = 80) String> permissionCodes,

        @Schema(description = "Contraseña", example = "SecurePass123*", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 6, max = 100)
        String password,

        @Schema(description = "Estado", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Boolean state
) {
    @JsonIgnore
    @Schema(hidden = true)
    public String resolvedRole() {
        if (role != null && !role.isBlank()) {
            return role.trim();
        }
        if (roleCode != null && !roleCode.isBlank()) {
            return roleCode.trim();
        }
        if (roleName != null && !roleName.isBlank()) {
            return roleName.trim();
        }
        return null;
    }

    @AssertTrue(message = "El rol es obligatorio (enviar role, roleCode o roleName)")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isRolePresent() {
        return resolvedRole() != null;
    }
}
