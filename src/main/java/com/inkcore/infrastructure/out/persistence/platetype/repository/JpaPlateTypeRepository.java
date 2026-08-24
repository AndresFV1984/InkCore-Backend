package com.inkcore.infrastructure.out.persistence.platetype.repository;

import com.inkcore.infrastructure.out.persistence.platetype.entity.PlateTypeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPlateTypeRepository extends JpaRepository<PlateTypeEntity, String> {

    Page<PlateTypeEntity> findAllByState(boolean state, Pageable pageable);

    Page<PlateTypeEntity> findAllByCompanyId(String companyId, Pageable pageable);

    Page<PlateTypeEntity> findAllByCompanyIdAndState(String companyId, boolean state, Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END
            FROM PlateTypeEntity p
            WHERE p.companyId = :companyId
              AND LOWER(TRIM(p.name)) = LOWER(TRIM(:name))
            """)
    boolean existsByCompanyIdAndNameIgnoreCase(
            @Param("companyId") String companyId,
            @Param("name") String name
    );

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END
            FROM PlateTypeEntity p
            WHERE p.companyId = :companyId
              AND LOWER(TRIM(p.name)) = LOWER(TRIM(:name))
              AND p.plateTypeId <> :plateTypeId
            """)
    boolean existsByCompanyIdAndNameIgnoreCaseAndPlateTypeIdNot(
            @Param("companyId") String companyId,
            @Param("name") String name,
            @Param("plateTypeId") String plateTypeId
    );
}
