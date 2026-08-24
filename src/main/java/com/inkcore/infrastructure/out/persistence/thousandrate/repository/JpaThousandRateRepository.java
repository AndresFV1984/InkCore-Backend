package com.inkcore.infrastructure.out.persistence.thousandrate.repository;

import com.inkcore.infrastructure.out.persistence.thousandrate.entity.ThousandRateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaThousandRateRepository extends JpaRepository<ThousandRateEntity, String> {

    Page<ThousandRateEntity> findAllByState(boolean state, Pageable pageable);

    Page<ThousandRateEntity> findAllByCompanyId(String companyId, Pageable pageable);

    Page<ThousandRateEntity> findAllByCompanyIdAndState(String companyId, boolean state, Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(t) > 0 THEN TRUE ELSE FALSE END
            FROM ThousandRateEntity t
            WHERE t.companyId = :companyId
              AND LOWER(TRIM(t.name)) = LOWER(TRIM(:name))
            """)
    boolean existsByCompanyIdAndNameIgnoreCase(
            @Param("companyId") String companyId,
            @Param("name") String name
    );

    @Query("""
            SELECT CASE WHEN COUNT(t) > 0 THEN TRUE ELSE FALSE END
            FROM ThousandRateEntity t
            WHERE t.companyId = :companyId
              AND LOWER(TRIM(t.name)) = LOWER(TRIM(:name))
              AND t.thousandRateId <> :thousandRateId
            """)
    boolean existsByCompanyIdAndNameIgnoreCaseAndThousandRateIdNot(
            @Param("companyId") String companyId,
            @Param("name") String name,
            @Param("thousandRateId") String thousandRateId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ThousandRateEntity t
            SET t.defaultFlag = FALSE
            WHERE t.companyId = :companyId
              AND LOWER(TRIM(t.colorCategory)) = LOWER(TRIM(:colorCategory))
              AND t.thousandRateId <> :thousandRateId
              AND t.defaultFlag = TRUE
            """)
    int clearDefaultForCompanyAndColorCategoryExcept(
            @Param("companyId") String companyId,
            @Param("colorCategory") String colorCategory,
            @Param("thousandRateId") String thousandRateId
    );
}
