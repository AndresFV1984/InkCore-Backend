package com.inkcore.infrastructure.out.persistence.client.repository;

import com.inkcore.infrastructure.out.persistence.client.entity.ClientEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaClientRepository extends JpaRepository<ClientEntity, String> {

    Page<ClientEntity> findAllByState(boolean state, Pageable pageable);

    Page<ClientEntity> findAllByCompanyId(String companyId, Pageable pageable);

    Page<ClientEntity> findAllByCompanyIdAndState(String companyId, boolean state, Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END
            FROM ClientEntity c
            WHERE c.companyId = :companyId
              AND LOWER(TRIM(c.identification)) = LOWER(TRIM(:identification))
            """)
    boolean existsByCompanyIdAndIdentificationIgnoreCase(
            @Param("companyId") String companyId,
            @Param("identification") String identification
    );

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END
            FROM ClientEntity c
            WHERE c.companyId = :companyId
              AND LOWER(TRIM(c.identification)) = LOWER(TRIM(:identification))
              AND c.clientId <> :clientId
            """)
    boolean existsByCompanyIdAndIdentificationIgnoreCaseAndClientIdNot(
            @Param("companyId") String companyId,
            @Param("identification") String identification,
            @Param("clientId") String clientId
    );
}
