package com.inkcore.infrastructure.out.persistence.user.repository;

import com.inkcore.infrastructure.out.persistence.user.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaUserRepository extends JpaRepository<UserEntity, String> {

    @Query("""
            SELECT DISTINCT u FROM UserEntity u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH r.permissions
            WHERE u.userId = :id
            """)
    Optional<UserEntity> findByIdWithRoles(@Param("id") String id);

    /**
     * Login: igualdad case-insensitive sobre mail ya normalizado (trim en adapter).
     * Evita TRIM() sobre la columna para poder usar índice funcional LOWER(mail).
     */
    @Query("""
            SELECT DISTINCT u FROM UserEntity u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH r.permissions
            WHERE LOWER(u.mail) = LOWER(:mail)
            """)
    Optional<UserEntity> findByMailFetchRoles(@Param("mail") String mail);

    @Query("""
            SELECT DISTINCT u FROM UserEntity u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH r.permissions
            WHERE u.userId IN :ids
            """)
    List<UserEntity> findAllByIdInWithRoles(@Param("ids") Collection<String> ids);

    Page<UserEntity> findAllByState(boolean state, Pageable pageable);

    @Query(value = "SELECT u.tokenVersion FROM UserEntity u WHERE u.userId = :id")
    Optional<Long> findTokenVersionByUserId(@Param("id") String id);

    boolean existsByMailIgnoreCase(String mail);

    boolean existsByMailIgnoreCaseAndUserIdNot(String mail, String userId);

    boolean existsByIdentificationNumber(String identificationNumber);

    boolean existsByIdentificationNumberAndUserIdNot(String identificationNumber, String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserEntity u SET u.lastLoginAt = :at WHERE u.userId = :id")
    void updateLastLoginAt(@Param("id") String id, @Param("at") LocalDateTime at);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE UserEntity u
            SET u.failedAttempts = :failedAttempts,
                u.lockedUntil = :lockedUntil,
                u.lastLoginAt = :lastLoginAt
            WHERE u.userId = :id
            """)
    void updateSecurityState(
            @Param("id") String id,
            @Param("failedAttempts") int failedAttempts,
            @Param("lockedUntil") LocalDateTime lockedUntil,
            @Param("lastLoginAt") LocalDateTime lastLoginAt
    );
}
