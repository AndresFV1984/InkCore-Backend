package com.inkcore.domain.user.ports.out;

import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.user.model.User;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(String userId);

    Optional<User> findByMailIgnoreCase(String mail);

    boolean existsByMailIgnoreCase(String mail);

    boolean existsByMailIgnoreCaseExcludingUserId(String mail, String userId);

    boolean existsByIdentificationNumber(String identificationNumber);

    boolean existsByIdentificationNumberExcludingUserId(String identificationNumber, String userId);

    PageResult<User> findPage(PageQuery pageQuery);

    PageResult<User> findPageByState(boolean state, PageQuery pageQuery);

    /** Versión de token actual para validar claim {@code tv} del JWT */
    Optional<Long> findTokenVersionByUserId(String userId);

    void updateLastLoginAt(String userId, LocalDateTime at);

    /**
     * Actualiza solo campos de seguridad de login (sin tocar roles ni el resto del perfil).
     */
    void updateSecurityState(
            String userId,
            int failedAttempts,
            LocalDateTime lockedUntil,
            LocalDateTime lastLoginAt
    );
}
