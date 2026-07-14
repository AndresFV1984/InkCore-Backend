package com.indicore.infrastructure.out.persistence.user.mapper;

import com.indicore.domain.user.model.User;
import com.indicore.infrastructure.out.persistence.user.entity.PermissionEntity;
import com.indicore.infrastructure.out.persistence.user.entity.RoleEntity;
import com.indicore.infrastructure.out.persistence.user.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        e.setContact(blankToNull(user.getContact()));
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

    public User toDomain(UserEntity entity) {
        RoleEntity role = entity.getRole();
        Set<PermissionEntity> permissions = entity.getPermissions() != null
                ? entity.getPermissions()
                : Set.of();
        List<String> permissionCodes = permissions.stream()
                .map(PermissionEntity::getCode)
                .sorted(Comparator.naturalOrder())
                .toList();

        return User.reconstitute(
                entity.getUserId(),
                entity.getCompanyId(),
                entity.getIdentificationNumber(),
                entity.getDocumentType(),
                entity.getName(),
                entity.getMail(),
                nullToEmpty(entity.getContact()),
                entity.getDepartment(),
                entity.getCity(),
                nullToEmpty(entity.getAddress()),
                entity.getPasswordHash(),
                entity.getCreationDate(),
                entity.isState(),
                entity.getTokenVersion(),
                role != null ? role.getRoleId() : null,
                role != null ? role.getCode() : "",
                role != null ? role.getName() : "",
                permissionCodes,
                entity.getForcePasswordChange(),
                entity.getPasswordChangedAt(),
                entity.getPasswordExpiresAt(),
                entity.getFailedAttempts() != null ? entity.getFailedAttempts() : 0,
                entity.getLockedUntil(),
                entity.getLastLoginAt()
        );
    }

    public void replacePermissions(UserEntity entity, Set<PermissionEntity> permissions) {
        if (entity.getPermissions() == null) {
            entity.setPermissions(new HashSet<>());
        }
        entity.getPermissions().clear();
        if (permissions != null) {
            entity.getPermissions().addAll(permissions);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
