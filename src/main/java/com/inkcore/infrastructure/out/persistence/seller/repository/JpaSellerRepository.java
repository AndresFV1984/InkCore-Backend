package com.inkcore.infrastructure.out.persistence.seller.repository;

import com.inkcore.infrastructure.out.persistence.seller.entity.SellerEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaSellerRepository extends JpaRepository<SellerEntity, String> {

    Page<SellerEntity> findAllByState(boolean state, Pageable pageable);

    Page<SellerEntity> findAllByCompanyId(String companyId, Pageable pageable);

    Page<SellerEntity> findAllByCompanyIdAndState(String companyId, boolean state, Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END
            FROM SellerEntity s
            WHERE s.companyId = :companyId
              AND LOWER(TRIM(s.identification)) = LOWER(TRIM(:identification))
            """)
    boolean existsByCompanyIdAndIdentificationIgnoreCase(
            @Param("companyId") String companyId,
            @Param("identification") String identification
    );

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END
            FROM SellerEntity s
            WHERE s.companyId = :companyId
              AND LOWER(TRIM(s.identification)) = LOWER(TRIM(:identification))
              AND s.sellerId <> :sellerId
            """)
    boolean existsByCompanyIdAndIdentificationIgnoreCaseAndSellerIdNot(
            @Param("companyId") String companyId,
            @Param("identification") String identification,
            @Param("sellerId") String sellerId
    );
}
