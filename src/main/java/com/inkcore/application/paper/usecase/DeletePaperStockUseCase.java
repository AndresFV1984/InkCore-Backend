package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.ports.out.PaperStockRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeletePaperStockUseCase {

    private final PaperStockRepositoryPort repository;
    private final PaperAccess access;
    private final PaperSupport support;

    public DeletePaperStockUseCase(
            PaperStockRepositoryPort repository,
            PaperAccess access,
            PaperSupport support
    ) {
        this.repository = repository;
        this.access = access;
        this.support = support;
    }

    @Transactional
    public void execute(String paperId, String paperStockId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        access.requirePaper(paperId, companyId);
        repository.findById(paperStockId)
                .filter(s -> companyId.equals(s.getCompanyId()) && paperId.equals(s.getPaperId()))
                .orElseThrow(() -> new ResourceNotFoundException("PAPER_STOCK_NOT_FOUND", "Lote de inventario no encontrado"));
        repository.deleteById(paperStockId);
    }
}
