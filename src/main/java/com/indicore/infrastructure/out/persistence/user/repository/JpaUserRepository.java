package com.indicore.infrastructure.out.persistence.user.repository;

import com.indicore.infrastructure.out.persistence.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface JpaUserRepository extends JpaRepository<UserEntity, String> {

    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.role WHERE u.userId = :id")
    java.util.Optional<UserEntity> findByIdWithRole(@Param("id") String id);

    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.role WHERE LOWER(TRIM(u.mail)) = LOWER(TRIM(:mail))")
    java.util.Optional<UserEntity> findByMailFetchRole(@Param("mail") String mail);

    @Query("SELECT DISTINCT u FROM UserEntity u LEFT JOIN FETCH u.role")
    java.util.List<UserEntity> findAllWithRole();

    boolean existsByMailIgnoreCase(String mail);

    boolean existsByIdentificationNumber(String identificationNumber);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserEntity u SET u.lastLoginAt = :at WHERE u.userId = :id")
    void updateLastLoginAt(@Param("id") String id, @Param("at") LocalDateTime at);
}
