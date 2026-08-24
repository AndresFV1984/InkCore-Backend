package com.inkcore.infrastructure.out.persistence.assemblyprice.repository;

import com.inkcore.infrastructure.out.persistence.assemblyprice.entity.AssemblyPriceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaAssemblyPriceRepository extends JpaRepository<AssemblyPriceEntity, String> {

    Page<AssemblyPriceEntity> findAllByState(boolean state, Pageable pageable);

    Page<AssemblyPriceEntity> findAllByCompanyId(String companyId, Pageable pageable);

    Page<AssemblyPriceEntity> findAllByCompanyIdAndState(String companyId, boolean state, Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END
            FROM AssemblyPriceEntity a
            WHERE a.companyId = :companyId
              AND LOWER(TRIM(a.name)) = LOWER(TRIM(:name))
            """)
    boolean existsByCompanyIdAndNameIgnoreCase(
            @Param("companyId") String companyId,
            @Param("name") String name
    );

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END
            FROM AssemblyPriceEntity a
            WHERE a.companyId = :companyId
              AND LOWER(TRIM(a.name)) = LOWER(TRIM(:name))
              AND a.assemblyPriceId <> :assemblyPriceId
            """)
    boolean existsByCompanyIdAndNameIgnoreCaseAndAssemblyPriceIdNot(
            @Param("companyId") String companyId,
            @Param("name") String name,
            @Param("assemblyPriceId") String assemblyPriceId
    );
}
