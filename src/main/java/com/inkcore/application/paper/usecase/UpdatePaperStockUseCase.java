package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.model.PaperStock;
import com.inkcore.domain.paper.ports.out.PaperStockRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class UpdatePaperStockUseCase {

    private final PaperStockRepositoryPort repository;
    private final PaperAccess access;
    private final PaperSupport support;
    private final Clock clock;

    public UpdatePaperStockUseCase(
            PaperStockRepositoryPort repository,
            PaperAccess access,
            PaperSupport support,
            Clock clock
    ) {
        this.repository = repository;
        this.access = access;
        this.support = support;
        this.clock = clock;
    }

    @Transactional
    public PaperStock execute(
            String paperId,
            String paperStockId,
            PaperCommands.UpdatePaperStockCommand command,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        access.requirePaper(paperId, companyId);
        PaperStock existing = repository.findById(paperStockId)
                .filter(s -> companyId.equals(s.getCompanyId()) && paperId.equals(s.getPaperId()))
                .orElseThrow(() -> new ResourceNotFoundException("PAPER_STOCK_NOT_FOUND", "Lote de inventario no encontrado"));
        PaperStock updated = existing.update(
                command.quantityInitial(),
                command.quantityAvailable(),
                command.unitCost(),
                command.entryDate(),
                command.state(),
                LocalDateTime.now(clock)
        );
        return repository.save(updated);
    }
}
