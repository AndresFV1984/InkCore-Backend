package com.inkcore.infrastructure.in.rest.users;

import com.inkcore.application.user.usecase.LoginResult;
import com.inkcore.domain.user.model.User;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "LoginResponse", description = """
        Usuario autenticado. Tokens en headers del envelope (token, refreshToken, expiraciones).
        documentType anidado; roles = [{ role, permissions }].
        """)
public record LoginResponse(
        @Schema(description = "Identificador del usuario", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String userId,

        @Schema(description = "Identificador de empresa", example = "company-seed-001")
        String companyId,

        @Schema(description = "Documento (tipo y número)")
        DocumentTypeResponse documentType,

        @Schema(description = "Nombre completo", example = "María Fernanda López")
        String name,

        @Schema(description = "true = activo", example = "true")
        boolean state,

        @Schema(description = "Debe cambiar la contraseña", example = "false")
        Boolean forcePasswordChange,

        LocalDateTime passwordChangedAt,
        LocalDateTime passwordExpiresAt,

        @Schema(description = "Intentos fallidos (0 tras login correcto)", example = "0")
        int failedAttempts,

        LocalDateTime lockedUntil,
        LocalDateTime lastLoginAt,

        PasswordExpirationWarningResponse passwordExpirationWarning,

        @ArraySchema(arraySchema = @Schema(description = "Roles del usuario con permisos"))
        List<RolePermissionsResponse> roles
) {
    public static LoginResponse from(LoginResult result) {
        User u = result.user();
        return new LoginResponse(
                u.getUserId(),
                u.getCompanyId(),
                DocumentTypeResponse.of(u.getDocumentType(), u.getIdentificationNumber()),
                u.getName(),
                u.isState(),
                u.getForcePasswordChange(),
                u.getPasswordChangedAt(),
                u.getPasswordExpiresAt(),
                u.getFailedAttempts(),
                u.getLockedUntil(),
                u.getLastLoginAt(),
                new PasswordExpirationWarningResponse(
                        result.passwordExpirationWarning().showWarning(),
                        result.passwordExpirationWarning().daysUntilExpiration()
                ),
                result.roles().stream()
                        .map(r -> new RolePermissionsResponse(r.role(), r.permissions()))
                        .toList()
        );
    }

    @Schema(name = "PasswordExpirationWarning")
    public record PasswordExpirationWarningResponse(
            @Schema(description = "true si la contraseña está cerca de expirar")
            boolean showWarning,
            @Schema(description = "Días restantes hasta la expiración", example = "5")
            long daysUntilExpiration
    ) {
    }

    @Schema(name = "LoginRolePermissions")
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
