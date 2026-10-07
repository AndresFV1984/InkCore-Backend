package com.inkcore.infrastructure.out.persistence.paper.repository;

import com.inkcore.infrastructure.out.persistence.paper.entity.PaperCutLayoutEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaPaperCutLayoutRepository extends JpaRepository<PaperCutLayoutEntity, String> {

    List<PaperCutLayoutEntity> findAllByCompanyIdAndPaperIdOrderByCreatedAtAsc(
            String companyId, String paperId);
}
