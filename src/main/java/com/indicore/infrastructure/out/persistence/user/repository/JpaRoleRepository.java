package com.indicore.infrastructure.out.persistence.user.repository;

import com.indicore.infrastructure.out.persistence.user.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaRoleRepository extends JpaRepository<RoleEntity, UUID> {

    Optional<RoleEntity> findByCodeIgnoreCase(String code);
}
