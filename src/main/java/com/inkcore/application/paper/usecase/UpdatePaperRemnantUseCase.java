package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.model.PaperRemnant;
import com.inkcore.domain.paper.model.PaperRemnantStatus;
import com.inkcore.domain.paper.ports.out.PaperRemnantRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class UpdatePaperRemnantUseCase {

    private final PaperRemnantRepositoryPort repository;
    private final PaperAccess access;
    private final PaperSupport support;
    private final Clock clock;

    public UpdatePaperRemnantUseCase(
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
            String paperRemnantId,
            PaperCommands.UpdatePaperRemnantCommand command,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        access.requirePaper(paperId, companyId);
        PaperRemnant existing = repository.findById(paperRemnantId)
                .filter(r -> companyId.equals(r.getCompanyId()) && paperId.equals(r.getPaperId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PAPER_REMNANT_NOT_FOUND", "Remanente de papel no encontrado"));
        PaperRemnantStatus status = resolveStatus(command, existing.getStatus());
        PaperRemnant updated = existing.update(
                command.width(),
                command.height(),
                command.unit(),
                command.quantityInitial(),
                command.quantityAvailable(),
                command.unitCost(),
                command.sourceProductionOrderId(),
                command.sourcePaperRowId(),
                command.entryDate(),
                command.note(),
                status,
                LocalDateTime.now(clock)
        );
        return repository.save(updated);
    }

    private static PaperRemnantStatus resolveStatus(
            PaperCommands.UpdatePaperRemnantCommand command,
            PaperRemnantStatus current
    ) {
        if (command.status() != null && !command.status().isBlank()) {
            return PaperRemnantStatus.fromWire(command.status());
        }
        if (command.state() != null) {
            return PaperRemnantStatus.fromLegacyState(command.state());
        }
        return current;
    }
}
