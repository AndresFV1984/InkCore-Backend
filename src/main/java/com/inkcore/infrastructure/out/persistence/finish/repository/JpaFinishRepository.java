package com.inkcore.infrastructure.out.persistence.finish.repository;

import com.inkcore.infrastructure.out.persistence.finish.entity.FinishEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaFinishRepository extends JpaRepository<FinishEntity, String> {

    Page<FinishEntity> findAllByState(boolean state, Pageable pageable);

    Page<FinishEntity> findAllByCompanyId(String companyId, Pageable pageable);

    Page<FinishEntity> findAllByCompanyIdAndState(String companyId, boolean state, Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(f) > 0 THEN TRUE ELSE FALSE END
            FROM FinishEntity f
            WHERE f.companyId = :companyId
              AND LOWER(TRIM(f.name)) = LOWER(TRIM(:name))
            """)
    boolean existsByCompanyIdAndNameIgnoreCase(
            @Param("companyId") String companyId,
            @Param("name") String name
    );

    @Query("""
            SELECT CASE WHEN COUNT(f) > 0 THEN TRUE ELSE FALSE END
            FROM FinishEntity f
            WHERE f.companyId = :companyId
              AND LOWER(TRIM(f.name)) = LOWER(TRIM(:name))
              AND f.finishId <> :finishId
            """)
    boolean existsByCompanyIdAndNameIgnoreCaseAndFinishIdNot(
            @Param("companyId") String companyId,
            @Param("name") String name,
            @Param("finishId") String finishId
    );
}
