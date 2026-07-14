package com.indicore.domain.user.model;

import java.util.Objects;
import java.util.UUID;

public final class Permission {

    private final UUID permissionId;
    private final String code;
    private final String name;

    private Permission(UUID permissionId, String code, String name) {
        this.permissionId = permissionId;
        this.code = code;
        this.name = name;
    }

    public static Permission reconstitute(UUID permissionId, String code, String name) {
        return new Permission(permissionId, code, name);
    }

    public UUID getPermissionId() {
        return permissionId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission that)) return false;
        return Objects.equals(permissionId, that.permissionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permissionId);
    }
}
