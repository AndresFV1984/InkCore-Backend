package com.inkcore.infrastructure.out.persistence.supplier.repository;

import com.inkcore.infrastructure.out.persistence.supplier.entity.SupplierEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaSupplierRepository extends JpaRepository<SupplierEntity, String> {

    Page<SupplierEntity> findAllByState(boolean state, Pageable pageable);

    Page<SupplierEntity> findAllByCompanyId(String companyId, Pageable pageable);

    Page<SupplierEntity> findAllByCompanyIdAndState(String companyId, boolean state, Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END
            FROM SupplierEntity s
            WHERE s.companyId = :companyId
              AND LOWER(TRIM(s.identification)) = LOWER(TRIM(:identification))
            """)
    boolean existsByCompanyIdAndIdentificationIgnoreCase(
            @Param("companyId") String companyId,
            @Param("identification") String identification
    );

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END
            FROM SupplierEntity s
            WHERE s.companyId = :companyId
              AND LOWER(TRIM(s.identification)) = LOWER(TRIM(:identification))
              AND s.supplierId <> :supplierId
            """)
    boolean existsByCompanyIdAndIdentificationIgnoreCaseAndSupplierIdNot(
            @Param("companyId") String companyId,
            @Param("identification") String identification,
            @Param("supplierId") String supplierId
    );
}
