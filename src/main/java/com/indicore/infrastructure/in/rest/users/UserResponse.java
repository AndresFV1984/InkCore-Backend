package com.indicore.infrastructure.in.rest.users;

import com.indicore.domain.user.model.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        String userId,
        String companyId,
        String identificationNumber,
        String documentType,
        String name,
        String mail,
        String contact,
        String address,
        LocalDate creationDate,
        boolean state,
        long tokenVersion,
        UUID roleId,
        String roleCode,
        Boolean forcePasswordChange,
        LocalDateTime passwordChangedAt,
        LocalDateTime passwordExpiresAt,
        int failedAttempts,
        LocalDateTime lockedUntil,
        LocalDateTime lastLoginAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getUserId(),
                u.getCompanyId(),
                u.getIdentificationNumber(),
                u.getDocumentType(),
                u.getName(),
                u.getMail(),
                u.getContact(),
                u.getAddress(),
                u.getCreationDate(),
                u.isState(),
                u.getTokenVersion(),
                u.getRoleId(),
                u.getRoleCode(),
                u.getForcePasswordChange(),
                u.getPasswordChangedAt(),
                u.getPasswordExpiresAt(),
                u.getFailedAttempts(),
                u.getLockedUntil(),
                u.getLastLoginAt()
        );
    }
}
