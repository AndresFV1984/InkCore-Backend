package com.inkcore.domain.user.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final List<UUID> roleIds;
    private final List<String> roleNames;
    private final List<String> roleCodes;
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
            List<UUID> roleIds,
            List<String> roleNames,
            List<String> roleCodes,
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
        this.roleIds = roleIds == null ? List.of() : List.copyOf(roleIds);
        this.roleNames = roleNames == null ? List.of() : List.copyOf(roleNames);
        this.roleCodes = roleCodes == null ? List.of() : List.copyOf(roleCodes);
        this.permissionCodes = permissionCodes == null ? List.of() : List.copyOf(permissionCodes);
        this.forcePasswordChange = forcePasswordChange;
        this.passwordChangedAt = passwordChangedAt;
        this.passwordExpiresAt = passwordExpiresAt;
        this.failedAttempts = failedAttempts;
        this.lockedUntil = lockedUntil;
        this.lastLoginAt = lastLoginAt;
    }

    public static String toRoleCode(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return "";
        }
        return roleName.trim().toUpperCase().replace(' ', '_');
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
            List<UUID> roleIds,
            List<String> roleNames,
            List<String> roleCodes,
            List<String> permissionCodes
    ) {
        requireNotBlank(name, "El nombre completo es obligatorio");
        requireNotBlank(documentType, "El tipo de documento es obligatorio");
        requireNotBlank(identificationNumber, "El número de identificación es obligatorio");
        requireNotBlank(mail, "El correo es obligatorio");
        requireNotBlank(department, "El departamento es obligatorio");
        requireNotBlank(city, "La ciudad / municipio es obligatorio");
        requireNotBlank(passwordHash, "La contraseña es obligatoria");

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
                roleIds,
                roleNames,
                roleCodes,
                permissionCodes,
                true,
                null,
                null,
                0,
                null,
                null
        );
    }

    /**
     * Alta vía API POST /api/v1/users: permite rol nulo, tokenVersion=0 y fechas de password.
     * Los campos de seguridad (lock, failedAttempts, etc.) se inicializan en el servidor.
     */
    public static User createForApi(
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
            List<UUID> roleIds,
            List<String> roleNames,
            List<String> roleCodes,
            List<String> permissionCodes,
            LocalDate creationDate,
            LocalDateTime passwordChangedAt,
            LocalDateTime passwordExpiresAt
    ) {
        requireNotBlank(name, "El nombre completo es obligatorio");
        requireNotBlank(identificationNumber, "El número de identificación es obligatorio");
        requireNotBlank(documentType, "El tipo de documento es obligatorio");
        requireNotBlank(mail, "El correo es obligatorio");
        requireNotBlank(department, "El departamento es obligatorio");
        requireNotBlank(city, "La ciudad / municipio es obligatorio");
        requireNotBlank(passwordHash, "La contraseña es obligatoria");

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
                creationDate,
                state,
                0L,
                roleIds == null ? List.of() : roleIds,
                roleNames == null ? List.of() : roleNames,
                roleCodes == null ? List.of() : roleCodes,
                permissionCodes == null ? List.of() : permissionCodes,
                true,
                passwordChangedAt,
                passwordExpiresAt,
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
            List<UUID> roleIds,
            List<String> roleNames,
            List<String> roleCodes,
            List<String> permissionCodes
    ) {
        requireNotBlank(name, "El nombre completo es obligatorio");
        requireNotBlank(documentType, "El tipo de documento es obligatorio");
        requireNotBlank(identificationNumber, "El número de identificación es obligatorio");
        requireNotBlank(mail, "El correo es obligatorio");
        requireNotBlank(department, "El departamento es obligatorio");
        requireNotBlank(city, "La ciudad / municipio es obligatorio");
        if (roleIds == null || roleIds.isEmpty()) {
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
                roleIds,
                roleNames,
                roleCodes,
                permissionCodes,
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
            List<UUID> roleIds,
            List<String> roleNames,
            List<String> roleCodes,
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
                roleIds,
                roleNames,
                roleCodes,
                permissionCodes,
                forcePasswordChange,
                passwordChangedAt,
                passwordExpiresAt,
                failedAttempts,
                lockedUntil,
                lastLoginAt
        );
    }

    /** Incrementa intentos fallidos y bloquea si supera el umbral. */
    public User registerFailedLogin(int maxFailedAttempts, int lockDurationMinutes, LocalDateTime now) {
        int nextAttempts = this.failedAttempts + 1;
        LocalDateTime nextLockedUntil = this.lockedUntil;
        if (nextAttempts >= maxFailedAttempts) {
            nextLockedUntil = now.plusMinutes(lockDurationMinutes);
        }
        return copyWithSecurityFields(nextAttempts, nextLockedUntil, this.lastLoginAt);
    }

    /** Resetea intentos/bloqueo y actualiza último login tras autenticación correcta. */
    public User registerSuccessfulLogin(LocalDateTime now) {
        return copyWithSecurityFields(0, null, now);
    }

    private User copyWithSecurityFields(int failedAttempts, LocalDateTime lockedUntil, LocalDateTime lastLoginAt) {
        return new User(
                this.userId,
                this.companyId,
                this.identificationNumber,
                this.documentType,
                this.name,
                this.mail,
                this.contact,
                this.department,
                this.city,
                this.address,
                this.passwordHash,
                this.creationDate,
                this.state,
                this.tokenVersion,
                this.roleIds,
                this.roleNames,
                this.roleCodes,
                this.permissionCodes,
                this.forcePasswordChange,
                this.passwordChangedAt,
                this.passwordExpiresAt,
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

    public List<UUID> getRoleIds() {
        return roleIds;
    }

    public List<String> getRoleNames() {
        return roleNames;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    /** Primer rol (compatibilidad con formulario de un solo rol). */
    public String getPrimaryRoleCode() {
        return roleCodes.isEmpty() ? "" : roleCodes.get(0);
    }

    public String getPrimaryRoleName() {
        return roleNames.isEmpty() ? "" : roleNames.get(0);
    }

    public UUID getPrimaryRoleId() {
        return roleIds.isEmpty() ? null : roleIds.get(0);
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
