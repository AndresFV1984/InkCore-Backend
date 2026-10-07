package com.inkcore.application.paper.usecase;

import com.inkcore.domain.cutlayout.ports.out.CutLayoutRepositoryPort;
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
public class CreatePaperCutLayoutUseCase {

    private final PaperCutLayoutRepositoryPort repository;
    private final CutLayoutRepositoryPort cutLayoutRepository;
    private final PaperAccess access;
    private final PaperSupport support;
    private final Clock clock;

    public CreatePaperCutLayoutUseCase(
            PaperCutLayoutRepositoryPort repository,
            CutLayoutRepositoryPort cutLayoutRepository,
            PaperAccess access,
            PaperSupport support,
            Clock clock
    ) {
        this.repository = repository;
        this.cutLayoutRepository = cutLayoutRepository;
        this.access = access;
        this.support = support;
        this.clock = clock;
    }

    @Transactional
    public PaperCutLayout execute(
            String paperId,
            PaperCommands.CreatePaperCutLayoutCommand command,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        access.requirePaper(paperId, companyId);
        cutLayoutRepository.findById(command.cutLayoutId())
                .filter(c -> companyId.equals(c.getCompanyId()))
                .orElseThrow(() -> new ResourceNotFoundException("CUT_LAYOUT_NOT_FOUND", "Despiece no encontrado"));
        PaperCutLayout layout = PaperCutLayout.createNew(
                companyId,
                paperId,
                command.cutLayoutId(),
                command.orientation(),
                command.wastePercentage(),
                command.note(),
                Objects.requireNonNullElse(command.state(), true),
                LocalDateTime.now(clock)
        );
        return repository.save(layout);
    }
}
