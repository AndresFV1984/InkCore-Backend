package com.inkcore.infrastructure.out.persistence.cutlayout.repository;

import com.inkcore.infrastructure.out.persistence.cutlayout.entity.CutLayoutEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaCutLayoutRepository extends JpaRepository<CutLayoutEntity, String> {

    Page<CutLayoutEntity> findAllByState(boolean state, Pageable pageable);

    Page<CutLayoutEntity> findAllByCompanyId(String companyId, Pageable pageable);

    Page<CutLayoutEntity> findAllByCompanyIdAndState(String companyId, boolean state, Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END
            FROM CutLayoutEntity c
            WHERE c.companyId = :companyId
              AND LOWER(TRIM(c.name)) = LOWER(TRIM(:name))
            """)
    boolean existsByCompanyIdAndNameIgnoreCase(
            @Param("companyId") String companyId,
            @Param("name") String name
    );

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END
            FROM CutLayoutEntity c
            WHERE c.companyId = :companyId
              AND LOWER(TRIM(c.name)) = LOWER(TRIM(:name))
              AND c.cutLayoutId <> :cutLayoutId
            """)
    boolean existsByCompanyIdAndNameIgnoreCaseAndCutLayoutIdNot(
            @Param("companyId") String companyId,
            @Param("name") String name,
            @Param("cutLayoutId") String cutLayoutId
    );
}
