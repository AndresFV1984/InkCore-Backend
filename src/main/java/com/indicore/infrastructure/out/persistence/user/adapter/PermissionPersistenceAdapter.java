package com.indicore.infrastructure.out.persistence.user.adapter;

import com.indicore.domain.user.model.Permission;
import com.indicore.domain.user.ports.out.PermissionRepositoryPort;
import com.indicore.infrastructure.out.persistence.user.entity.PermissionEntity;
import com.indicore.infrastructure.out.persistence.user.repository.JpaPermissionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Component
public class PermissionPersistenceAdapter implements PermissionRepositoryPort {

    private final JpaPermissionRepository jpaPermissionRepository;

    public PermissionPersistenceAdapter(JpaPermissionRepository jpaPermissionRepository) {
        this.jpaPermissionRepository = jpaPermissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> findAll() {
        return jpaPermissionRepository.findAllByOrderByNameAsc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> findByCodes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        return jpaPermissionRepository.findByCodesIgnoreCase(
                        codes.stream().map(c -> c.trim().toUpperCase()).toList()
                ).stream()
                .map(this::toDomain)
                .toList();
    }

    private Permission toDomain(PermissionEntity entity) {
        return Permission.reconstitute(entity.getPermissionId(), entity.getCode(), entity.getName());
    }
}
