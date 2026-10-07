package com.inkcore.infrastructure.out.persistence.paper.repository;

import com.inkcore.infrastructure.out.persistence.paper.entity.PaperRemnantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaPaperRemnantRepository extends JpaRepository<PaperRemnantEntity, String> {

    List<PaperRemnantEntity> findAllByCompanyIdAndPaperIdOrderByEntryDateDesc(
            String companyId, String paperId);

    List<PaperRemnantEntity> findAllByCompanyIdAndPaperIdAndStateOrderByEntryDateDesc(
            String companyId, String paperId, boolean state);
}
