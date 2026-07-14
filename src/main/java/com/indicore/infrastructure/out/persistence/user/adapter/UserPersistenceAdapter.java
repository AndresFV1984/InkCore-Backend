package com.indicore.infrastructure.out.persistence.user.adapter;

import com.indicore.domain.user.model.User;
import com.indicore.domain.user.ports.out.UserRepositoryPort;
import com.indicore.infrastructure.out.persistence.user.entity.PermissionEntity;
import com.indicore.infrastructure.out.persistence.user.entity.UserEntity;
import com.indicore.infrastructure.out.persistence.user.mapper.UserPersistenceMapper;
import com.indicore.infrastructure.out.persistence.user.repository.JpaPermissionRepository;
import com.indicore.infrastructure.out.persistence.user.repository.JpaRoleRepository;
import com.indicore.infrastructure.out.persistence.user.repository.JpaUserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final JpaUserRepository jpaUserRepository;
    private final JpaRoleRepository jpaRoleRepository;
    private final JpaPermissionRepository jpaPermissionRepository;
    private final UserPersistenceMapper mapper;

    public UserPersistenceAdapter(
            JpaUserRepository jpaUserRepository,
            JpaRoleRepository jpaRoleRepository,
            JpaPermissionRepository jpaPermissionRepository,
            UserPersistenceMapper mapper
    ) {
        this.jpaUserRepository = jpaUserRepository;
        this.jpaRoleRepository = jpaRoleRepository;
        this.jpaPermissionRepository = jpaPermissionRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = jpaUserRepository.findById(user.getUserId()).orElse(null);
        if (entity == null) {
            entity = mapper.toNewEntity(user);
        } else {
            mapper.copyScalars(user, entity);
        }

        if (user.getRoleId() != null) {
            jpaRoleRepository.findById(user.getRoleId()).ifPresent(entity::setRole);
        } else {
            entity.setRole(null);
        }

        Set<PermissionEntity> permissions = resolvePermissions(user.getPermissionCodes());
        mapper.replacePermissions(entity, permissions);

        UserEntity saved = jpaUserRepository.save(entity);
        return jpaUserRepository.findByIdWithRole(saved.getUserId())
                .map(mapper::toDomain)
                .orElseGet(() -> mapper.toDomain(saved));
    }

    private Set<PermissionEntity> resolvePermissions(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(jpaPermissionRepository.findByCodesIgnoreCase(
                codes.stream().map(String::toUpperCase).toList()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(String userId) {
        return jpaUserRepository.findByIdWithRole(userId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByMailIgnoreCase(String mail) {
        if (mail == null || mail.isBlank()) {
            return Optional.empty();
        }
        return jpaUserRepository.findByMailFetchRole(mail.trim()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByMailIgnoreCase(String mail) {
        return mail != null && jpaUserRepository.existsByMailIgnoreCase(mail.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByMailIgnoreCaseExcludingUserId(String mail, String userId) {
        return mail != null
                && jpaUserRepository.existsByMailIgnoreCaseAndUserIdNot(mail.trim(), userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByIdentificationNumber(String identificationNumber) {
        return identificationNumber != null
                && jpaUserRepository.existsByIdentificationNumber(identificationNumber.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByIdentificationNumberExcludingUserId(String identificationNumber, String userId) {
        return identificationNumber != null
                && jpaUserRepository.existsByIdentificationNumberAndUserIdNot(
                identificationNumber.trim(),
                userId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return jpaUserRepository.findAllWithRole().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findTokenVersionByUserId(String userId) {
        return jpaUserRepository.findById(userId).map(UserEntity::getTokenVersion);
    }

    @Override
    @Transactional
    public void updateLastLoginAt(String userId, LocalDateTime at) {
        jpaUserRepository.updateLastLoginAt(userId, at);
    }
}
