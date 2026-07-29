package com.inkcore.infrastructure.out.persistence.finishingprocess.repository;

import com.inkcore.infrastructure.out.persistence.finishingprocess.entity.FinishingProcessEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaFinishingProcessRepository extends JpaRepository<FinishingProcessEntity, String> {

    Page<FinishingProcessEntity> findAllByState(boolean state, Pageable pageable);

    Page<FinishingProcessEntity> findAllByCompanyId(String companyId, Pageable pageable);

    Page<FinishingProcessEntity> findAllByCompanyIdAndState(String companyId, boolean state, Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(f) > 0 THEN TRUE ELSE FALSE END
            FROM FinishingProcessEntity f
            WHERE f.companyId = :companyId
              AND LOWER(TRIM(f.name)) = LOWER(TRIM(:name))
            """)
    boolean existsByCompanyIdAndNameIgnoreCase(
            @Param("companyId") String companyId,
            @Param("name") String name
    );

    @Query("""
            SELECT CASE WHEN COUNT(f) > 0 THEN TRUE ELSE FALSE END
            FROM FinishingProcessEntity f
            WHERE f.companyId = :companyId
              AND LOWER(TRIM(f.name)) = LOWER(TRIM(:name))
              AND f.finishingProcessId <> :finishingProcessId
            """)
    boolean existsByCompanyIdAndNameIgnoreCaseAndFinishingProcessIdNot(
            @Param("companyId") String companyId,
            @Param("name") String name,
            @Param("finishingProcessId") String finishingProcessId
    );
}
