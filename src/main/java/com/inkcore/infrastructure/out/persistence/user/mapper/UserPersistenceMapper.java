package com.inkcore.infrastructure.out.persistence.user.mapper;

import com.inkcore.domain.user.model.User;
import com.inkcore.infrastructure.out.persistence.user.entity.PermissionEntity;
import com.inkcore.infrastructure.out.persistence.user.entity.RoleEntity;
import com.inkcore.infrastructure.out.persistence.user.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class UserPersistenceMapper {

    public UserEntity toNewEntity(User user) {
        UserEntity e = new UserEntity();
        copyScalars(user, e);
        return e;
    }

    public void copyScalars(User user, UserEntity e) {
        e.setUserId(user.getUserId());
        e.setCompanyId(user.getCompanyId());
        e.setIdentificationNumber(user.getIdentificationNumber());
        e.setDocumentType(user.getDocumentType());
        e.setName(user.getName());
        e.setMail(user.getMail());
        e.setContact(user.getContact() == null ? "" : user.getContact());
        e.setDepartment(user.getDepartment());
        e.setCity(user.getCity());
        e.setAddress(blankToNull(user.getAddress()));
        e.setPasswordHash(user.getPasswordHash());
        e.setCreationDate(user.getCreationDate());
        e.setState(user.isState());
        e.setTokenVersion(user.getTokenVersion());
        e.setForcePasswordChange(user.getForcePasswordChange());
        e.setPasswordChangedAt(user.getPasswordChangedAt());
        e.setPasswordExpiresAt(user.getPasswordExpiresAt());
        e.setFailedAttempts(user.getFailedAttempts());
        e.setLockedUntil(user.getLockedUntil());
        e.setLastLoginAt(user.getLastLoginAt());
    }

    public void replaceRoles(UserEntity entity, Set<RoleEntity> roles) {
        if (entity.getRoles() == null) {
            entity.setRoles(new HashSet<>());
        }
        entity.getRoles().clear();
        if (roles != null) {
            entity.getRoles().addAll(roles);
        }
    }

    public User toDomain(UserEntity entity) {
        Set<RoleEntity> roles = entity.getRoles() != null ? entity.getRoles() : Set.of();
        List<RoleEntity> ordered = roles.stream()
                .sorted(Comparator.comparing(RoleEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<UUID> roleIds = ordered.stream().map(RoleEntity::getRoleId).toList();
        List<String> roleNames = ordered.stream().map(RoleEntity::getName).toList();
        List<String> roleCodes = ordered.stream().map(r -> User.toRoleCode(r.getName())).toList();
        // Permisos distinct por rol (JOIN FETCH evita N+1); la unión flattened se usa en JWT/compat.
        List<String> permissionCodes = ordered.stream()
                .flatMap(r -> permissionsOf(r).stream())
                .distinct()
                .sorted()
                .toList();

        return User.reconstitute(
                entity.getUserId(),
                entity.getCompanyId(),
                entity.getIdentificationNumber(),
                entity.getDocumentType(),
                entity.getName(),
                entity.getMail(),
                entity.getContact() == null ? "" : entity.getContact(),
                entity.getDepartment(),
                entity.getCity(),
                entity.getAddress() == null ? "" : entity.getAddress(),
                entity.getPasswordHash(),
                entity.getCreationDate(),
                entity.isState(),
                entity.getTokenVersion(),
                roleIds,
                roleNames,
                roleCodes,
                permissionCodes,
                entity.getForcePasswordChange(),
                entity.getPasswordChangedAt(),
                entity.getPasswordExpiresAt(),
                entity.getFailedAttempts() != null ? entity.getFailedAttempts() : 0,
                entity.getLockedUntil(),
                entity.getLastLoginAt()
        );
    }

    /** Permisos distinct de un rol (ya cargados vía JOIN FETCH). */
    public static List<String> permissionsOf(RoleEntity role) {
        if (role.getPermissions() == null || role.getPermissions().isEmpty()) {
            return List.of();
        }
        return role.getPermissions().stream()
                .map(PermissionEntity::getCode)
                .distinct()
                .sorted()
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
