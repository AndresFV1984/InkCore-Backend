package com.inkcore.domain.user.model;

import java.util.Objects;
import java.util.UUID;

public final class Permission {

    private final UUID permissionId;
    private final String code;
    private final String name;
    private final String module;
    private final String description;

    private Permission(UUID permissionId, String code, String name, String module, String description) {
        this.permissionId = permissionId;
        this.code = code;
        this.name = name;
        this.module = module != null ? module : "";
        this.description = description != null ? description : "";
    }

    public static Permission reconstitute(
            UUID permissionId,
            String code,
            String name,
            String module,
            String description
    ) {
        return new Permission(permissionId, code, name, module, description);
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

    public String getModule() {
        return module;
    }

    public String getDescription() {
        return description;
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
