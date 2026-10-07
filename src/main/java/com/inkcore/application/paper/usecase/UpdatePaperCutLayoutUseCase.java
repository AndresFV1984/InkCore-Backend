package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.model.PaperCutLayout;
import com.inkcore.domain.paper.ports.out.PaperCutLayoutRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class UpdatePaperCutLayoutUseCase {

    private final PaperCutLayoutRepositoryPort repository;
    private final PaperAccess access;
    private final PaperSupport support;
    private final Clock clock;

    public UpdatePaperCutLayoutUseCase(
            PaperCutLayoutRepositoryPort repository,
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
    public PaperCutLayout execute(
            String paperId,
            String paperCutLayoutId,
            PaperCommands.UpdatePaperCutLayoutCommand command,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        access.requirePaper(paperId, companyId);
        PaperCutLayout existing = repository.findById(paperCutLayoutId)
                .filter(l -> companyId.equals(l.getCompanyId()) && paperId.equals(l.getPaperId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PAPER_CUT_LAYOUT_NOT_FOUND", "Despiece del papel no encontrado"));
        PaperCutLayout updated = existing.update(
                command.orientation(),
                command.wastePercentage(),
                command.note(),
                Objects.requireNonNullElse(command.state(), existing.isState()),
                LocalDateTime.now(clock)
        );
        return repository.save(updated);
    }
}
