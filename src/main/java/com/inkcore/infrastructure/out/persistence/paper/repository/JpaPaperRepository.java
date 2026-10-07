package com.inkcore.infrastructure.out.persistence.paper.repository;

import com.inkcore.infrastructure.out.persistence.paper.entity.PaperEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface JpaPaperRepository extends JpaRepository<PaperEntity, String> {

    @Query("""
            SELECT p FROM PaperEntity p
            WHERE p.companyId = :companyId
              AND (:state IS NULL OR p.state = :state)
              AND (:coated IS NULL OR p.coated = :coated)
              AND (:acceptsRemnants IS NULL OR p.acceptsRemnants = :acceptsRemnants)
            """)
    Page<PaperEntity> findPage(
            @Param("companyId") String companyId,
            @Param("state") Boolean state,
            @Param("coated") Boolean coated,
            @Param("acceptsRemnants") Boolean acceptsRemnants,
            Pageable pageable
    );

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END
            FROM PaperEntity p
            WHERE p.companyId = :companyId
              AND LOWER(TRIM(p.name)) = LOWER(TRIM(:name))
              AND ((p.grammage IS NULL AND :grammage IS NULL) OR p.grammage = :grammage)
              AND p.width = :width
              AND p.height = :height
              AND LOWER(p.unit) = LOWER(:unit)
            """)
    boolean existsDuplicate(
            @Param("companyId") String companyId,
            @Param("name") String name,
            @Param("grammage") BigDecimal grammage,
            @Param("width") BigDecimal width,
            @Param("height") BigDecimal height,
            @Param("unit") String unit
    );

    @Query("""
            SELECT CASE WHEN COUNT(p) > 0 THEN TRUE ELSE FALSE END
            FROM PaperEntity p
            WHERE p.companyId = :companyId
              AND LOWER(TRIM(p.name)) = LOWER(TRIM(:name))
              AND ((p.grammage IS NULL AND :grammage IS NULL) OR p.grammage = :grammage)
              AND p.width = :width
              AND p.height = :height
              AND LOWER(p.unit) = LOWER(:unit)
              AND p.paperId <> :paperId
            """)
    boolean existsDuplicateExcludingId(
            @Param("companyId") String companyId,
            @Param("name") String name,
            @Param("grammage") BigDecimal grammage,
            @Param("width") BigDecimal width,
            @Param("height") BigDecimal height,
            @Param("unit") String unit,
            @Param("paperId") String paperId
    );
}
