package com.indicore.infrastructure.out.persistence.user.repository;

import com.indicore.infrastructure.out.persistence.user.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface JpaPermissionRepository extends JpaRepository<PermissionEntity, UUID> {

    @Query("SELECT p FROM PermissionEntity p WHERE UPPER(p.code) IN :codes")
    List<PermissionEntity> findByCodesIgnoreCase(@Param("codes") Collection<String> codes);

    List<PermissionEntity> findAllByOrderByNameAsc();
}
