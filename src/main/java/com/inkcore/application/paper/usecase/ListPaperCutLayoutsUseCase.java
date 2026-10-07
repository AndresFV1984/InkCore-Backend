package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.model.PaperCutLayout;
import com.inkcore.domain.paper.ports.out.PaperCutLayoutRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListPaperCutLayoutsUseCase {

    private final PaperCutLayoutRepositoryPort repository;
    private final PaperAccess access;
    private final PaperSupport support;

    public ListPaperCutLayoutsUseCase(
            PaperCutLayoutRepositoryPort repository,
            PaperAccess access,
            PaperSupport support
    ) {
        this.repository = repository;
        this.access = access;
        this.support = support;
    }

    @Transactional(readOnly = true)
    public List<PaperCutLayout> execute(String paperId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        access.requirePaper(paperId, companyId);
        return repository.findByPaperId(companyId, paperId);
    }
}
