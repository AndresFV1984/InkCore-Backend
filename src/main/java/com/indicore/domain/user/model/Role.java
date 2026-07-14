package com.indicore.domain.user.model;

import java.util.Objects;
import java.util.UUID;

public final class Role {

    private final UUID roleId;
    private final String code;
    private final String name;

    private Role(UUID roleId, String code, String name) {
        this.roleId = roleId;
        this.code = code;
        this.name = name;
    }

    public static Role reconstitute(UUID roleId, String code, String name) {
        return new Role(roleId, code, name);
    }

    public UUID getRoleId() {
        return roleId;
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
        if (!(o instanceof Role role)) return false;
        return Objects.equals(roleId, role.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId);
    }
}
