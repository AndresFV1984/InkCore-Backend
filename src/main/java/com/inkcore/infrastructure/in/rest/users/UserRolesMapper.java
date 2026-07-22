package com.inkcore.infrastructure.in.rest.users;

import com.inkcore.domain.user.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * Mapea roles del dominio al formato API:
 * {@code [ { "role": "SUPERVISOR", "permissions": ["PERMISSION_..."] } ]}.
 */
final class UserRolesMapper {

    private UserRolesMapper() {
    }

    static List<UserResponse.RolePermissionsResponse> map(User u) {
        List<String> roleCodes = u.getRoleCodes();
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of();
        }
        List<String> permissions = u.getPermissionCodes() == null ? List.of() : List.copyOf(u.getPermissionCodes());
        List<UserResponse.RolePermissionsResponse> roles = new ArrayList<>(roleCodes.size());
        for (String roleCode : roleCodes) {
            roles.add(new UserResponse.RolePermissionsResponse(roleCode, permissions));
        }
        return List.copyOf(roles);
    }

    static List<UserListItemResponse.RolePermissionsResponse> mapForList(User u) {
        return map(u).stream()
                .map(r -> new UserListItemResponse.RolePermissionsResponse(r.role(), r.permissions()))
                .toList();
    }
}
