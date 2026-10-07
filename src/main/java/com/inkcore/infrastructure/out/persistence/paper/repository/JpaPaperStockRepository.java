package com.inkcore.infrastructure.out.persistence.paper.repository;

import com.inkcore.infrastructure.out.persistence.paper.entity.PaperStockEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaPaperStockRepository extends JpaRepository<PaperStockEntity, String> {

    List<PaperStockEntity> findAllByCompanyIdAndPaperIdOrderByEntryDateDesc(
            String companyId, String paperId);

    List<PaperStockEntity> findAllByCompanyIdAndPaperIdAndStateOrderByEntryDateDesc(
            String companyId, String paperId, boolean state);
}
