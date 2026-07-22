package com.inkcore.infrastructure.out.persistence.user.adapter;

import com.inkcore.domain.user.model.Role;
import com.inkcore.domain.user.ports.out.RoleRepositoryPort;
import com.inkcore.infrastructure.out.persistence.user.entity.PermissionEntity;
import com.inkcore.infrastructure.out.persistence.user.entity.RoleEntity;
import com.inkcore.infrastructure.out.persistence.user.repository.JpaRoleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class RolePersistenceAdapter implements RoleRepositoryPort {

    private final JpaRoleRepository jpaRoleRepository;

    public RolePersistenceAdapter(JpaRoleRepository jpaRoleRepository) {
        this.jpaRoleRepository = jpaRoleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return jpaRoleRepository.findAllWithPermissions().stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(Role::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findById(UUID roleId) {
        return jpaRoleRepository.findByIdWithPermissions(roleId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findByCodeOrName(String codeOrName) {
        if (codeOrName == null || codeOrName.isBlank()) {
            return Optional.empty();
        }
        return jpaRoleRepository.findByCodeOrName(codeOrName.trim()).map(this::toDomain);
    }

    private Role toDomain(RoleEntity entity) {
        Set<PermissionEntity> permissions = entity.getPermissions() != null
                ? entity.getPermissions()
                : Set.of();
        List<String> codes = permissions.stream()
                .map(PermissionEntity::getCode)
                .sorted()
                .toList();
        return Role.reconstitute(
                entity.getRoleId(),
                entity.getCompanyId(),
                entity.getName(),
                entity.getDescription(),
                entity.isState(),
                codes
        );
    }
}
