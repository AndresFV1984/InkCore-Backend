package com.inkcore.infrastructure.out.persistence.papertype.repository;

import com.inkcore.infrastructure.out.persistence.papertype.entity.PaperTypeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPaperTypeRepository extends JpaRepository<PaperTypeEntity, String> {

    Page<PaperTypeEntity> findAllByState(boolean state, Pageable pageable);

    Page<PaperTypeEntity> findAllByCompanyId(String companyId, Pageable pageable);

    Page<PaperTypeEntity> findAllByCompanyIdAndState(String companyId, boolean state, Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END
            FROM PaperTypeEntity p
            WHERE p.companyId = :companyId
              AND LOWER(TRIM(p.name)) = LOWER(TRIM(:name))
            """)
    boolean existsByCompanyIdAndNameIgnoreCase(
            @Param("companyId") String companyId,
            @Param("name") String name
    );

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END
            FROM PaperTypeEntity p
            WHERE p.companyId = :companyId
              AND LOWER(TRIM(p.name)) = LOWER(TRIM(:name))
              AND p.paperTypeId <> :paperTypeId
            """)
    boolean existsByCompanyIdAndNameIgnoreCaseAndPaperTypeIdNot(
            @Param("companyId") String companyId,
            @Param("name") String name,
            @Param("paperTypeId") String paperTypeId
    );
}
