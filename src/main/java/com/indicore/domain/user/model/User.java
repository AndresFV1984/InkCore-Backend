package com.indicore.domain.user.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private final String department;
    private final String city;
    private final String address;
    private final String passwordHash;
    private final LocalDate creationDate;
    private final boolean state;
    private final long tokenVersion;
    private final UUID roleId;
    private final String roleCode;
    private final String roleName;
    private final List<String> permissionCodes;
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
            String department,
            String city,
            String address,
            String passwordHash,
            LocalDate creationDate,
            boolean state,
            long tokenVersion,
            UUID roleId,
            String roleCode,
            String roleName,
            List<String> permissionCodes,
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
        this.contact = contact != null ? contact : "";
        this.department = department;
        this.city = city;
        this.address = address != null ? address : "";
        this.passwordHash = passwordHash;
        this.creationDate = creationDate;
        this.state = state;
        this.tokenVersion = tokenVersion;
        this.roleId = roleId;
        this.roleCode = roleCode != null ? roleCode : "";
        this.roleName = roleName != null ? roleName : "";
        this.permissionCodes = permissionCodes == null
                ? List.of()
                : List.copyOf(permissionCodes);
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
            String department,
            String city,
            String address,
            String passwordHash,
            boolean state,
            UUID roleId,
            String roleCode,
            String roleName,
            List<String> permissionCodes
    ) {
        requireNotBlank(name, "El nombre completo es obligatorio");
        requireNotBlank(documentType, "El tipo de documento es obligatorio");
        requireNotBlank(identificationNumber, "El número de identificación es obligatorio");
        requireNotBlank(mail, "El correo es obligatorio");
        requireNotBlank(department, "El departamento es obligatorio");
        requireNotBlank(city, "La ciudad / municipio es obligatorio");
        requireNotBlank(passwordHash, "La contraseña es obligatoria");
        if (roleId == null) {
            throw new IllegalArgumentException("El rol es obligatorio");
        }

        return new User(
                UUID.randomUUID().toString(),
                blankToDefault(companyId, "company-seed-001"),
                identificationNumber.trim(),
                documentType.trim().toUpperCase(),
                name.trim(),
                mail.trim().toLowerCase(),
                blankToEmpty(contact),
                department.trim(),
                city.trim(),
                blankToEmpty(address),
                passwordHash,
                LocalDate.now(),
                state,
                1L,
                roleId,
                roleCode,
                roleName,
                normalizePermissionCodes(permissionCodes),
                true,
                null,
                null,
                0,
                null,
                null
        );
    }

    public User update(
            String identificationNumber,
            String documentType,
            String name,
            String mail,
            String contact,
            String department,
            String city,
            String address,
            String passwordHashOrNull,
            boolean state,
            UUID roleId,
            String roleCode,
            String roleName,
            List<String> permissionCodes
    ) {
        requireNotBlank(name, "El nombre completo es obligatorio");
        requireNotBlank(documentType, "El tipo de documento es obligatorio");
        requireNotBlank(identificationNumber, "El número de identificación es obligatorio");
        requireNotBlank(mail, "El correo es obligatorio");
        requireNotBlank(department, "El departamento es obligatorio");
        requireNotBlank(city, "La ciudad / municipio es obligatorio");
        if (roleId == null) {
            throw new IllegalArgumentException("El rol es obligatorio");
        }

        String nextHash = passwordHashOrNull == null || passwordHashOrNull.isBlank()
                ? this.passwordHash
                : passwordHashOrNull;

        return new User(
                this.userId,
                this.companyId,
                identificationNumber.trim(),
                documentType.trim().toUpperCase(),
                name.trim(),
                mail.trim().toLowerCase(),
                blankToEmpty(contact),
                department.trim(),
                city.trim(),
                blankToEmpty(address),
                nextHash,
                this.creationDate,
                state,
                this.tokenVersion,
                roleId,
                roleCode,
                roleName,
                normalizePermissionCodes(permissionCodes),
                this.forcePasswordChange,
                passwordHashOrNull != null && !passwordHashOrNull.isBlank()
                        ? LocalDateTime.now()
                        : this.passwordChangedAt,
                this.passwordExpiresAt,
                this.failedAttempts,
                this.lockedUntil,
                this.lastLoginAt
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
            String department,
            String city,
            String address,
            String passwordHash,
            LocalDate creationDate,
            boolean state,
            long tokenVersion,
            UUID roleId,
            String roleCode,
            String roleName,
            List<String> permissionCodes,
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
                department,
                city,
                address,
                passwordHash,
                creationDate,
                state,
                tokenVersion,
                roleId,
                roleCode,
                roleName,
                permissionCodes,
                forcePasswordChange,
                passwordChangedAt,
                passwordExpiresAt,
                failedAttempts,
                lockedUntil,
                lastLoginAt
        );
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static List<String> normalizePermissionCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String code : codes) {
            if (code != null && !code.isBlank()) {
                String trimmed = code.trim().toUpperCase();
                if (!normalized.contains(trimmed)) {
                    normalized.add(trimmed);
                }
            }
        }
        return List.copyOf(normalized);
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

    public String getDepartment() {
        return department;
    }

    public String getCity() {
        return city;
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

    public String getRoleName() {
        return roleName;
    }

    public List<String> getPermissionCodes() {
        return permissionCodes;
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
