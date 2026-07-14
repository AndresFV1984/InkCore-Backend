package com.indicore.infrastructure.out.persistence.user.adapter;

import com.indicore.domain.user.model.Role;
import com.indicore.domain.user.ports.out.RoleRepositoryPort;
import com.indicore.infrastructure.out.persistence.user.entity.RoleEntity;
import com.indicore.infrastructure.out.persistence.user.repository.JpaRoleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class RolePersistenceAdapter implements RoleRepositoryPort {

    private final JpaRoleRepository jpaRoleRepository;

    public RolePersistenceAdapter(JpaRoleRepository jpaRoleRepository) {
        this.jpaRoleRepository = jpaRoleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return jpaRoleRepository.findAll().stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(Role::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findByCodeIgnoreCase(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return jpaRoleRepository.findByCodeIgnoreCase(code.trim()).map(this::toDomain);
    }

    private Role toDomain(RoleEntity entity) {
        return Role.reconstitute(entity.getRoleId(), entity.getCode(), entity.getName());
    }
}
