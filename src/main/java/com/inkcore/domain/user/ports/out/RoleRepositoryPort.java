package com.inkcore.domain.user.ports.out;

import com.inkcore.domain.user.model.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepositoryPort {

    List<Role> findAll();

    Optional<Role> findById(UUID roleId);

    /** Busca por código de autorización (ADMINISTRADOR) o por nombre (Administrador). */
    Optional<Role> findByCodeOrName(String codeOrName);

    /** Alias de búsqueda por nombre de rol (ej. SUPERVISOR / Administrador). */
    default Optional<Role> findByRoleName(String roleName) {
        return findByCodeOrName(roleName);
    }
}
