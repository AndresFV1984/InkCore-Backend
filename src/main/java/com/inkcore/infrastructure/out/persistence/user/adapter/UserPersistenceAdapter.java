package com.inkcore.infrastructure.out.persistence.user.adapter;

import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.user.model.User;
import com.inkcore.domain.user.ports.out.UserRepositoryPort;
import com.inkcore.infrastructure.out.persistence.user.entity.UserEntity;
import com.inkcore.infrastructure.out.persistence.user.mapper.UserPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.user.repository.JpaRoleRepository;
import com.inkcore.infrastructure.out.persistence.user.repository.JpaUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final JpaUserRepository jpaUserRepository;
    private final JpaRoleRepository jpaRoleRepository;
    private final UserPersistenceMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

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
        List<UUID> roleIds = user.getRoleIds() == null ? List.of() : user.getRoleIds();

        UserEntity existing = jpaUserRepository.findById(user.getUserId()).orElse(null);
        if (existing == null) {
            UserEntity entity = mapper.toNewEntity(user);
            // No gestionar roles vía @ManyToMany: la join table tiene assigned_at.
            entity.setRoles(new HashSet<>());
            entityManager.persist(entity);
        } else {
            mapper.copyScalars(user, existing);
            if (existing.getRoles() != null) {
                existing.getRoles().clear();
            }
        }
        entityManager.flush();
        // Evita que Hibernate, con la colección roles vacía, borre los INSERT de user_roles al commit.
        entityManager.clear();

        String userId = user.getUserId();
        entityManager.createNativeQuery("DELETE FROM indicolors.user_roles WHERE user_id = :userId")
                .setParameter("userId", userId)
                .executeUpdate();

        for (UUID roleId : roleIds) {
            if (!jpaRoleRepository.existsById(roleId)) {
                throw new IllegalStateException("Rol no encontrado al persistir usuario: " + roleId);
            }
            int inserted = entityManager.createNativeQuery("""
                            INSERT INTO indicolors.user_roles (user_id, role_id, assigned_at)
                            VALUES (:userId, :roleId, now())
                            """)
                    .setParameter("userId", userId)
                    .setParameter("roleId", roleId)
                    .executeUpdate();
            if (inserted != 1) {
                throw new IllegalStateException(
                        "No se insertó user_roles para userId=" + userId + ", roleId=" + roleId
                );
            }
        }
        entityManager.flush();
        entityManager.clear();

        User saved = jpaUserRepository.findByIdWithRoles(userId)
                .map(mapper::toDomain)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado tras guardar: " + userId));
        if (!roleIds.isEmpty() && saved.getRoleIds().isEmpty()) {
            throw new IllegalStateException("user_roles no quedó persistido para userId=" + userId);
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(String userId) {
        return jpaUserRepository.findByIdWithRoles(userId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByMailIgnoreCase(String mail) {
        if (mail == null || mail.isBlank()) {
            return Optional.empty();
        }
        return jpaUserRepository.findByMailFetchRoles(mail.trim().toLowerCase()).map(mapper::toDomain);
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
    public PageResult<User> findPage(PageQuery pageQuery) {
        PageRequest pageable = PageRequest.of(
                pageQuery.page(),
                pageQuery.size(),
                Sort.by(Sort.Direction.ASC, "name")
        );
        Page<UserEntity> page = jpaUserRepository.findAll(pageable);
        return toPageResult(page, pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<User> findPageByState(boolean state, PageQuery pageQuery) {
        PageRequest pageable = PageRequest.of(
                pageQuery.page(),
                pageQuery.size(),
                Sort.by(Sort.Direction.ASC, "name")
        );
        Page<UserEntity> page = jpaUserRepository.findAllByState(state, pageable);
        return toPageResult(page, pageQuery);
    }

    private PageResult<User> toPageResult(Page<UserEntity> page, PageQuery pageQuery) {
        List<String> ids = page.getContent().stream().map(UserEntity::getUserId).toList();
        if (ids.isEmpty()) {
            return new PageResult<>(List.of(), pageQuery.page(), pageQuery.size(), page.getTotalElements());
        }
        Map<String, UserEntity> withRoles = jpaUserRepository.findAllByIdInWithRoles(ids).stream()
                .collect(Collectors.toMap(UserEntity::getUserId, Function.identity(), (a, b) -> a));
        List<User> content = ids.stream()
                .map(withRoles::get)
                .filter(e -> e != null)
                .map(mapper::toDomain)
                .toList();
        return new PageResult<>(content, pageQuery.page(), pageQuery.size(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findTokenVersionByUserId(String userId) {
        return jpaUserRepository.findTokenVersionByUserId(userId);
    }

    @Override
    @Transactional
    public void updateLastLoginAt(String userId, LocalDateTime at) {
        jpaUserRepository.updateLastLoginAt(userId, at);
    }

    @Override
    @Transactional
    public void updateSecurityState(
            String userId,
            int failedAttempts,
            LocalDateTime lockedUntil,
            LocalDateTime lastLoginAt
    ) {
        jpaUserRepository.updateSecurityState(userId, failedAttempts, lockedUntil, lastLoginAt);
    }
}
