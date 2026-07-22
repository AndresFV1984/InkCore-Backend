package com.inkcore.domain.user.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Role {

    private final UUID roleId;
    private final String companyId;
    private final String name;
    private final String description;
    private final boolean state;
    private final List<String> permissionCodes;

    private Role(
            UUID roleId,
            String companyId,
            String name,
            String description,
            boolean state,
            List<String> permissionCodes
    ) {
        this.roleId = roleId;
        this.companyId = companyId;
        this.name = name;
        this.description = description != null ? description : "";
        this.state = state;
        this.permissionCodes = permissionCodes == null ? List.of() : List.copyOf(permissionCodes);
    }

    public static Role reconstitute(
            UUID roleId,
            String companyId,
            String name,
            String description,
            boolean state,
            List<String> permissionCodes
    ) {
        return new Role(roleId, companyId, name, description, state, permissionCodes);
    }

    public String getCode() {
        return User.toRoleCode(name);
    }

    public UUID getRoleId() {
        return roleId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isState() {
        return state;
    }

    public List<String> getPermissionCodes() {
        return permissionCodes;
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
