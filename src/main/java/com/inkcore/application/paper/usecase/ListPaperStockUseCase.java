package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.model.PaperStock;
import com.inkcore.domain.paper.ports.out.PaperStockRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListPaperStockUseCase {

    private final PaperStockRepositoryPort repository;
    private final PaperAccess access;
    private final PaperSupport support;

    public ListPaperStockUseCase(
            PaperStockRepositoryPort repository,
            PaperAccess access,
            PaperSupport support
    ) {
        this.repository = repository;
        this.access = access;
        this.support = support;
    }

    @Transactional(readOnly = true)
    public List<PaperStock> execute(String paperId, Boolean state, Authentication authentication) {
        String companyId = support.companyId(authentication);
        access.requirePaper(paperId, companyId);
        return repository.findByPaperId(companyId, paperId, state);
    }
}
