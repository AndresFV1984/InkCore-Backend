package com.inkcore.infrastructure.out.persistence.user.adapter;

import com.inkcore.domain.user.model.Permission;
import com.inkcore.domain.user.ports.out.PermissionRepositoryPort;
import com.inkcore.infrastructure.out.persistence.user.entity.PermissionEntity;
import com.inkcore.infrastructure.out.persistence.user.repository.JpaPermissionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Component
public class PermissionPersistenceAdapter implements PermissionRepositoryPort {

    private final JpaPermissionRepository jpaPermissionRepository;

    public PermissionPersistenceAdapter(JpaPermissionRepository jpaPermissionRepository) {
        this.jpaPermissionRepository = jpaPermissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> findAll() {
        return jpaPermissionRepository.findAllByOrderByModuleAscNameAsc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permission> findByCodes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        List<String> normalized = codes.stream()
                .filter(c -> c != null && !c.isBlank())
                .map(c -> c.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
        return jpaPermissionRepository.findByCodesIgnoreCase(normalized).stream()
                .map(this::toDomain)
                .toList();
    }

    private Permission toDomain(PermissionEntity entity) {
        return Permission.reconstitute(
                entity.getPermissionId(),
                entity.getCode(),
                entity.getName(),
                entity.getModule(),
                entity.getDescription()
        );
    }
}
