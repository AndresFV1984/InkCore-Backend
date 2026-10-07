package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.model.PaperStock;
import com.inkcore.domain.paper.ports.out.PaperStockRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class CreatePaperStockUseCase {

    private final PaperStockRepositoryPort repository;
    private final PaperAccess access;
    private final PaperSupport support;
    private final Clock clock;

    public CreatePaperStockUseCase(
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
            PaperCommands.CreatePaperStockCommand command,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        access.requirePaper(paperId, companyId);
        PaperStock stock = PaperStock.createNew(
                companyId,
                paperId,
                command.quantityInitial(),
                command.quantityAvailable(),
                command.unitCost(),
                Objects.requireNonNullElse(command.entryDate(), LocalDate.now(clock)),
                command.state(),
                LocalDateTime.now(clock)
        );
        return repository.save(stock);
    }
}
