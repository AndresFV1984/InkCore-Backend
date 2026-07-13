package com.indicore.infrastructure.out.persistence.user.adapter;

import com.indicore.domain.user.model.User;
import com.indicore.domain.user.ports.out.UserRepositoryPort;
import com.indicore.infrastructure.out.persistence.user.entity.UserEntity;
import com.indicore.infrastructure.out.persistence.user.mapper.UserPersistenceMapper;
import com.indicore.infrastructure.out.persistence.user.repository.JpaRoleRepository;
import com.indicore.infrastructure.out.persistence.user.repository.JpaUserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final JpaUserRepository jpaUserRepository;
    private final JpaRoleRepository jpaRoleRepository;
    private final UserPersistenceMapper mapper;

    public UserPersistenceAdapter(
            JpaUserRepository jpaUserRepository,
            JpaRoleRepository jpaRoleRepository,
            UserPersistenceMapper mapper
    ) {
        this.jpaUserRepository = jpaUserRepository;
        this.jpaRoleRepository = jpaRoleRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = mapper.toNewEntity(user);
        if (user.getRoleId() != null) {
            jpaRoleRepository.findById(user.getRoleId()).ifPresent(entity::setRole);
        }
        UserEntity saved = jpaUserRepository.save(entity);
        return mapper.toDomain(saved);
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
    public boolean existsByIdentificationNumber(String identificationNumber) {
        return identificationNumber != null
                && jpaUserRepository.existsByIdentificationNumber(identificationNumber.trim());
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
