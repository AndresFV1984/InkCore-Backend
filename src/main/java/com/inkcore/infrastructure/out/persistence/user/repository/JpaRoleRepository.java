package com.inkcore.infrastructure.out.persistence.user.repository;

import com.inkcore.infrastructure.out.persistence.user.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaRoleRepository extends JpaRepository<RoleEntity, UUID> {

    @Query("""
            SELECT DISTINCT r FROM RoleEntity r
            LEFT JOIN FETCH r.permissions
            WHERE r.roleId = :id
            """)
    Optional<RoleEntity> findByIdWithPermissions(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT r FROM RoleEntity r
            LEFT JOIN FETCH r.permissions
            """)
    List<RoleEntity> findAllWithPermissions();

    @Query("""
            SELECT DISTINCT r FROM RoleEntity r
            LEFT JOIN FETCH r.permissions
            WHERE UPPER(REPLACE(r.name, ' ', '_')) = UPPER(:code)
               OR LOWER(r.name) = LOWER(:code)
            """)
    Optional<RoleEntity> findByCodeOrName(@Param("code") String code);
}
