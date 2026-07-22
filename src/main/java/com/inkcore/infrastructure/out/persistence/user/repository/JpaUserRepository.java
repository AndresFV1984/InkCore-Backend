package com.inkcore.infrastructure.out.persistence.user.repository;

import com.inkcore.infrastructure.out.persistence.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface JpaUserRepository extends JpaRepository<UserEntity, String> {

    @Query("""
            SELECT DISTINCT u FROM UserEntity u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH r.permissions
            WHERE u.userId = :id
            """)
    java.util.Optional<UserEntity> findByIdWithRoles(@Param("id") String id);

    @Query("""
            SELECT DISTINCT u FROM UserEntity u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH r.permissions
            WHERE LOWER(TRIM(u.mail)) = LOWER(TRIM(:mail))
            """)
    java.util.Optional<UserEntity> findByMailFetchRoles(@Param("mail") String mail);

    @Query("""
            SELECT DISTINCT u FROM UserEntity u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH r.permissions
            """)
    java.util.List<UserEntity> findAllWithRoles();

    @Query("""
            SELECT DISTINCT u FROM UserEntity u
            LEFT JOIN FETCH u.roles r
            LEFT JOIN FETCH r.permissions
            WHERE u.state = :state
            """)
    java.util.List<UserEntity> findAllByStateWithRoles(@Param("state") boolean state);

    boolean existsByMailIgnoreCase(String mail);

    boolean existsByMailIgnoreCaseAndUserIdNot(String mail, String userId);

    boolean existsByIdentificationNumber(String identificationNumber);

    boolean existsByIdentificationNumberAndUserIdNot(String identificationNumber, String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserEntity u SET u.lastLoginAt = :at WHERE u.userId = :id")
    void updateLastLoginAt(@Param("id") String id, @Param("at") LocalDateTime at);
}
