package com.indicore.infrastructure.out.persistence.user.mapper;

import com.indicore.domain.user.model.User;
import com.indicore.infrastructure.out.persistence.user.entity.RoleEntity;
import com.indicore.infrastructure.out.persistence.user.entity.UserEntity;
import org.springframework.stereotype.Component;

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
        e.setContact(user.getContact());
        e.setAddress(user.getAddress());
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
        return User.reconstitute(
                entity.getUserId(),
                entity.getCompanyId(),
                entity.getIdentificationNumber(),
                entity.getDocumentType(),
                entity.getName(),
                entity.getMail(),
                entity.getContact(),
                entity.getAddress(),
                entity.getPasswordHash(),
                entity.getCreationDate(),
                entity.isState(),
                entity.getTokenVersion(),
                role != null ? role.getRoleId() : null,
                role != null ? role.getCode() : "",
                entity.getForcePasswordChange(),
                entity.getPasswordChangedAt(),
                entity.getPasswordExpiresAt(),
                entity.getFailedAttempts() != null ? entity.getFailedAttempts() : 0,
                entity.getLockedUntil(),
                entity.getLastLoginAt()
        );
    }
}
