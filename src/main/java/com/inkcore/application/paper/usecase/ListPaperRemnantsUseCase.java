package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.model.PaperRemnant;
import com.inkcore.domain.paper.ports.out.PaperRemnantRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListPaperRemnantsUseCase {

    private final PaperRemnantRepositoryPort repository;
    private final PaperAccess access;
    private final PaperSupport support;

    public ListPaperRemnantsUseCase(
            PaperRemnantRepositoryPort repository,
            PaperAccess access,
            PaperSupport support
    ) {
        this.repository = repository;
        this.access = access;
        this.support = support;
    }

    @Transactional(readOnly = true)
    public List<PaperRemnant> execute(String paperId, Boolean state, Authentication authentication) {
        String companyId = support.companyId(authentication);
        access.requirePaper(paperId, companyId);
        return repository.findByPaperId(companyId, paperId, state);
    }
}
