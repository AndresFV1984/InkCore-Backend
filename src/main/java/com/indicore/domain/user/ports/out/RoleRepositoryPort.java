package com.indicore.domain.user.ports.out;

import com.indicore.domain.user.model.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepositoryPort {

    List<Role> findAll();

    Optional<Role> findByCodeIgnoreCase(String code);
}
