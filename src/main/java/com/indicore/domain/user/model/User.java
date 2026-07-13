package com.indicore.domain.user.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado usuario. {@code passwordHash} solo circula hacia persistencia — no exponga en API.
 */
public final class User {

    private final String userId;
    private final String companyId;
    private final String identificationNumber;
    private final String documentType;
    private final String name;
    private final String mail;
    private final String contact;
    private final String address;
    private final String passwordHash;
    private final LocalDate creationDate;
    private final boolean state;
    private final long tokenVersion;
    private final UUID roleId;
    /** Código de rol para emitir JWT (p. ej. ADMINISTRADOR); vacío si no hay rol. */
    private final String roleCode;
    private final Boolean forcePasswordChange;
    private final LocalDateTime passwordChangedAt;
    private final LocalDateTime passwordExpiresAt;
    private final int failedAttempts;
    private final LocalDateTime lockedUntil;
    private final LocalDateTime lastLoginAt;

    private User(
            String userId,
            String companyId,
            String identificationNumber,
            String documentType,
            String name,
            String mail,
            String contact,
            String address,
            String passwordHash,
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
        this.userId = userId;
        this.companyId = companyId;
        this.identificationNumber = identificationNumber;
        this.documentType = documentType;
        this.name = name;
        this.mail = mail;
        this.contact = contact;
        this.address = address;
        this.passwordHash = passwordHash;
        this.creationDate = creationDate;
        this.state = state;
        this.tokenVersion = tokenVersion;
        this.roleId = roleId;
        this.roleCode = roleCode != null ? roleCode : "";
        this.forcePasswordChange = forcePasswordChange;
        this.passwordChangedAt = passwordChangedAt;
        this.passwordExpiresAt = passwordExpiresAt;
        this.failedAttempts = failedAttempts;
        this.lockedUntil = lockedUntil;
        this.lastLoginAt = lastLoginAt;
    }

    public static User createNew(
            String companyId,
            String identificationNumber,
            String documentType,
            String name,
            String mail,
            String contact,
            String address,
            String passwordHash,
            UUID roleId,
            String roleCode
    ) {
        if (mail == null || mail.isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
        return new User(
                UUID.randomUUID().toString(),
                companyId,
                identificationNumber,
                documentType,
                name,
                mail.trim().toLowerCase(),
                contact,
                address,
                passwordHash,
                LocalDate.now(),
                true,
                1L,
                roleId,
                roleCode != null ? roleCode : "",
                true,
                null,
                null,
                0,
                null,
                null
        );
    }

    public static User reconstitute(
            String userId,
            String companyId,
            String identificationNumber,
            String documentType,
            String name,
            String mail,
            String contact,
            String address,
            String passwordHash,
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
        return new User(
                userId,
                companyId,
                identificationNumber,
                documentType,
                name,
                mail,
                contact,
                address,
                passwordHash,
                creationDate,
                state,
                tokenVersion,
                roleId,
                roleCode,
                forcePasswordChange,
                passwordChangedAt,
                passwordExpiresAt,
                failedAttempts,
                lockedUntil,
                lastLoginAt
        );
    }

    public String getUserId() {
        return userId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getIdentificationNumber() {
        return identificationNumber;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getName() {
        return name;
    }

    public String getMail() {
        return mail;
    }

    public String getContact() {
        return contact;
    }

    public String getAddress() {
        return address;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public boolean isState() {
        return state;
    }

    public long getTokenVersion() {
        return tokenVersion;
    }

    public UUID getRoleId() {
        return roleId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public Boolean getForcePasswordChange() {
        return forcePasswordChange;
    }

    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public LocalDateTime getPasswordExpiresAt() {
        return passwordExpiresAt;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(userId, user.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
