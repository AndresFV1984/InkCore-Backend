package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.model.PaperRemnant;
import com.inkcore.domain.paper.model.PaperRemnantStatus;
import com.inkcore.domain.paper.ports.out.PaperRemnantRepositoryPort;
import com.inkcore.domain.productionorder.ports.out.ProductionOrderRepositoryPort;
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
    private final ProductionOrderRepositoryPort productionOrderRepository;
    private final PaperAccess access;
    private final PaperSupport support;
    private final Clock clock;

    public CreatePaperRemnantUseCase(
            PaperRemnantRepositoryPort repository,
            ProductionOrderRepositoryPort productionOrderRepository,
            PaperAccess access,
            PaperSupport support,
            Clock clock
    ) {
        this.repository = repository;
        this.productionOrderRepository = productionOrderRepository;
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
        PaperRemnantStatus status = resolveInitialStatus(companyId, command);
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
                status,
                LocalDateTime.now(clock)
        );
        return repository.save(remnant);
    }

    /**
     * Prioridad: status explícito → state legacy → estado de la OP origen (COMPLETED→AVAILABLE)
     * → sin OP: {@link PaperRemnantStatus#PENDING} (misma regla que OP nueva).
     */
    private PaperRemnantStatus resolveInitialStatus(
            String companyId,
            PaperCommands.CreatePaperRemnantCommand command
    ) {
        if (command.status() != null && !command.status().isBlank()) {
            return PaperRemnantStatus.fromWire(command.status());
        }
        if (command.state() != null) {
            return PaperRemnantStatus.fromLegacyState(command.state());
        }
        String sourceOrderId = blankToNull(command.sourceProductionOrderId());
        if (sourceOrderId == null) {
            return PaperRemnantStatus.PENDING;
        }
        return productionOrderRepository.findById(sourceOrderId)
                .filter(order -> companyId.equals(order.getCompanyId()))
                .map(order -> PaperRemnantStatus.fromProductionOrderStatusWire(order.getStatus()))
                .orElse(PaperRemnantStatus.PENDING);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
