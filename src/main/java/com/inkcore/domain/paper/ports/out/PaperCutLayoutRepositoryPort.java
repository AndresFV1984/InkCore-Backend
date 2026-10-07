package com.inkcore.domain.paper.ports.out;

import com.inkcore.domain.paper.model.PaperCutLayout;

import java.util.List;
import java.util.Optional;

public interface PaperCutLayoutRepositoryPort {

    PaperCutLayout save(PaperCutLayout layout);

    Optional<PaperCutLayout> findById(String paperCutLayoutId);

    List<PaperCutLayout> findByPaperId(String companyId, String paperId);

    void deleteById(String paperCutLayoutId);
}
