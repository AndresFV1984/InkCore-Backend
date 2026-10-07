package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.model.PaperRemnant;
import com.inkcore.domain.paper.ports.out.PaperRemnantRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class CreatePaperRemnantUseCase {

    private final PaperRemnantRepositoryPort repository;
    private final PaperAccess access;
    private final PaperSupport support;
    private final Clock clock;

    public CreatePaperRemnantUseCase(
            PaperRemnantRepositoryPort repository,
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
    public PaperRemnant execute(
            String paperId,
            PaperCommands.CreatePaperRemnantCommand command,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        access.requirePaper(paperId, companyId);
        PaperRemnant remnant = PaperRemnant.createNew(
                companyId,
                paperId,
                command.width(),
                command.height(),
                command.unit(),
                command.quantityInitial(),
                command.quantityAvailable(),
                command.unitCost(),
                command.sourceProductionOrderId(),
                command.sourcePaperRowId(),
                Objects.requireNonNullElse(command.entryDate(), LocalDate.now(clock)),
                command.note(),
                command.state(),
                LocalDateTime.now(clock)
        );
        return repository.save(remnant);
    }
}
