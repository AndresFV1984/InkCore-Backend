package com.indicore.infrastructure.in.rest.users;

import com.indicore.domain.user.model.User;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(name = "UserResponse", description = "Usuario del sistema (sin contraseña)")
public record UserResponse(
        @Schema(description = "Identificador único del usuario", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String userId,

        @Schema(description = "Identificador de empresa", example = "company-seed-001")
        String companyId,

        @Schema(description = "Nombre completo", example = "María Fernanda López")
        String name,

        @Schema(description = "Tipo de documento", example = "CC")
        String documentType,

        @Schema(description = "Número de identificación", example = "1234567890")
        String identificationNumber,

        @Schema(description = "Correo electrónico", example = "usuario@empresa.com")
        String mail,

        @Schema(description = "Teléfono / contacto", example = "300 123 4567")
        String contact,

        @Schema(description = "Departamento", example = "Antioquia")
        String department,

        @Schema(description = "Ciudad / municipio", example = "Medellín")
        String city,

        @Schema(description = "Dirección", example = "Calle 10 # 20-30")
        String address,

        @Schema(description = "UUID del rol asignado", example = "b1ffbc99-9c0b-4ef8-bb6d-6bb9bd380a22")
        UUID roleId,

        @Schema(description = "Código del rol", example = "OPERADOR")
        String roleCode,

        @Schema(description = "Nombre visible del rol", example = "Operador")
        String roleName,

        @ArraySchema(
                arraySchema = @Schema(description = "Permisos asignados al usuario"),
                schema = @Schema(example = "VER_ORDENES_PRODUCCION")
        )
        List<String> permissionCodes,

        @Schema(description = "true = Activo, false = Inactivo", example = "true")
        boolean state,

        @Schema(description = "Fecha de creación", example = "2026-07-13")
        LocalDate creationDate,

        @Schema(description = "Versión de token JWT (claim tv)", example = "1")
        long tokenVersion,

        @Schema(description = "Indica si debe cambiar la contraseña en el próximo acceso", example = "true")
        Boolean forcePasswordChange,

        @Schema(description = "Último cambio de contraseña")
        LocalDateTime passwordChangedAt,

        @Schema(description = "Fecha de expiración de contraseña")
        LocalDateTime passwordExpiresAt,

        @Schema(description = "Intentos fallidos de login", example = "0")
        int failedAttempts,

        @Schema(description = "Bloqueo vigente hasta (si aplica)")
        LocalDateTime lockedUntil,

        @Schema(description = "Último inicio de sesión")
        LocalDateTime lastLoginAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getUserId(),
                u.getCompanyId(),
                u.getName(),
                u.getDocumentType(),
                u.getIdentificationNumber(),
                u.getMail(),
                u.getContact(),
                u.getDepartment(),
                u.getCity(),
                u.getAddress(),
                u.getRoleId(),
                u.getRoleCode(),
                u.getRoleName(),
                u.getPermissionCodes(),
                u.isState(),
                u.getCreationDate(),
                u.getTokenVersion(),
                u.getForcePasswordChange(),
                u.getPasswordChangedAt(),
                u.getPasswordExpiresAt(),
                u.getFailedAttempts(),
                u.getLockedUntil(),
                u.getLastLoginAt()
        );
    }
}
