package com.inkcore.infrastructure.out.persistence.paper.repository;

import com.inkcore.infrastructure.out.persistence.paper.entity.PaperPriceHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaPaperPriceHistoryRepository extends JpaRepository<PaperPriceHistoryEntity, String> {

    List<PaperPriceHistoryEntity> findAllByCompanyIdAndPaperIdOrderByEffectiveFromDesc(
            String companyId, String paperId);
}
