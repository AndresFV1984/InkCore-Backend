package com.inkcore.infrastructure.in.rest.users;

import com.inkcore.domain.user.model.User;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detalle de usuario (create/get/update/profile).
 * No expone tokenVersion, roleId, roleCode, roleName ni permissionCodes planos.
 */
@Schema(name = "UserResponse", description = """
        Usuario del sistema (sin contraseña).
        documentType y department van anidados; roles = [{ role, permissions }].
        No incluye tokenVersion / roleId / roleCode / roleName / permissionCodes planos.
        """)
public record UserResponse(
        @Schema(description = "Identificador único del usuario", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String userId,

        @Schema(description = "Identificador de empresa", example = "company-seed-001")
        String companyId,

        @Schema(description = "Documento (tipo y número)")
        DocumentTypeResponse documentType,

        @Schema(description = "Nombre completo", example = "María Fernanda López")
        String name,

        @Schema(description = "Correo electrónico", example = "usuario@empresa.com")
        String mail,

        @Schema(description = "Teléfono / contacto", example = "300 123 4567")
        String contact,

        @Schema(description = "Ubicación (departamento y ciudad)")
        DepartmentResponse department,

        @Schema(description = "Dirección", example = "Calle 10 # 20-30")
        String address,

        @Schema(description = "Fecha de creación", example = "2026-07-14")
        LocalDate creationDate,

        @Schema(description = "true = Activo, false = Inactivo", example = "true")
        boolean state,

        @Schema(description = "Indica si debe cambiar la contraseña", example = "true")
        Boolean forcePasswordChange,

        LocalDateTime passwordChangedAt,
        LocalDateTime passwordExpiresAt,

        @Schema(description = "Intentos fallidos de login", example = "0")
        int failedAttempts,

        LocalDateTime lockedUntil,
        LocalDateTime lastLoginAt,

        @ArraySchema(
                arraySchema = @Schema(description = "Roles del usuario con sus permisos"),
                schema = @Schema(implementation = RolePermissionsResponse.class)
        )
        List<RolePermissionsResponse> roles
) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getUserId(),
                u.getCompanyId(),
                DocumentTypeResponse.of(u.getDocumentType(), u.getIdentificationNumber()),
                u.getName(),
                u.getMail(),
                u.getContact(),
                DepartmentResponse.of(u.getDepartment(), u.getCity()),
                u.getAddress(),
                u.getCreationDate(),
                u.isState(),
                u.getForcePasswordChange(),
                u.getPasswordChangedAt(),
                u.getPasswordExpiresAt(),
                u.getFailedAttempts(),
                u.getLockedUntil(),
                u.getLastLoginAt(),
                UserRolesMapper.map(u)
        );
    }

    @Schema(name = "UserRolePermissions")
    public record RolePermissionsResponse(
            @Schema(description = "Código del rol", example = "SUPERVISOR")
            String role,
            @ArraySchema(
                    arraySchema = @Schema(description = "Permisos del rol"),
                    schema = @Schema(example = "PERMISSION_USUARIO_VER")
            )
            List<String> permissions
    ) {
    }
}
