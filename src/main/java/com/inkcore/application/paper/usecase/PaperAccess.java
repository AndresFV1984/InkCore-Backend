package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.model.Paper;
import com.inkcore.domain.paper.ports.out.PaperRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class PaperAccess {

    private final PaperRepositoryPort paperRepository;
    private final PaperSupport support;

    public PaperAccess(PaperRepositoryPort paperRepository, PaperSupport support) {
        this.paperRepository = paperRepository;
        this.support = support;
    }

    public Paper requirePaper(String paperId, String companyId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("PAPER_NOT_FOUND", "Papel no encontrado"));
        support.requireSameCompany(paper.getCompanyId(), companyId);
        return paper;
    }
}
