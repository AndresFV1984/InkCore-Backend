package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.model.PaperCutLayout;
import com.inkcore.domain.paper.ports.out.PaperCutLayoutRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPaperCutLayoutByIdUseCase {

    private final PaperCutLayoutRepositoryPort repository;
    private final PaperAccess access;
    private final PaperSupport support;

    public GetPaperCutLayoutByIdUseCase(
            PaperCutLayoutRepositoryPort repository,
            PaperAccess access,
            PaperSupport support
    ) {
        this.repository = repository;
        this.access = access;
        this.support = support;
    }

    @Transactional(readOnly = true)
    public PaperCutLayout execute(String paperId, String paperCutLayoutId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        access.requirePaper(paperId, companyId);
        return repository.findById(paperCutLayoutId)
                .filter(l -> companyId.equals(l.getCompanyId()) && paperId.equals(l.getPaperId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PAPER_CUT_LAYOUT_NOT_FOUND", "Despiece del papel no encontrado"));
    }
}
