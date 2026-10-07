package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.model.Paper;
import com.inkcore.domain.paper.ports.out.PaperRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPaperByIdUseCase {

    private final PaperRepositoryPort paperRepository;
    private final PaperSupport support;

    public GetPaperByIdUseCase(PaperRepositoryPort paperRepository, PaperSupport support) {
        this.paperRepository = paperRepository;
        this.support = support;
    }

    @Transactional(readOnly = true)
    public Paper execute(String paperId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("PAPER_NOT_FOUND", "Papel no encontrado"));
        support.requireSameCompany(paper.getCompanyId(), companyId);
        return paper;
    }
}
