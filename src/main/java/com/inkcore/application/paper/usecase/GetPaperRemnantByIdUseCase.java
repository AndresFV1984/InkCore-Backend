package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.model.PaperRemnant;
import com.inkcore.domain.paper.ports.out.PaperRemnantRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPaperRemnantByIdUseCase {

    private final PaperRemnantRepositoryPort repository;
    private final PaperAccess access;
    private final PaperSupport support;

    public GetPaperRemnantByIdUseCase(
            PaperRemnantRepositoryPort repository,
            PaperAccess access,
            PaperSupport support
    ) {
        this.repository = repository;
        this.access = access;
        this.support = support;
    }

    @Transactional(readOnly = true)
    public PaperRemnant execute(String paperId, String paperRemnantId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        access.requirePaper(paperId, companyId);
        return repository.findById(paperRemnantId)
                .filter(r -> companyId.equals(r.getCompanyId()) && paperId.equals(r.getPaperId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PAPER_REMNANT_NOT_FOUND", "Remanente de papel no encontrado"));
    }
}
