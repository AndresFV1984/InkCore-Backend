package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.exception.PaperAlreadyExistsException;
import com.inkcore.domain.paper.model.Paper;
import com.inkcore.domain.paper.ports.out.PaperRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class UpdatePaperUseCase {

    private final PaperRepositoryPort paperRepository;
    private final PaperSupport support;
    private final Clock clock;

    public UpdatePaperUseCase(PaperRepositoryPort paperRepository, PaperSupport support, Clock clock) {
        this.paperRepository = paperRepository;
        this.support = support;
        this.clock = clock;
    }

    @Transactional
    public Paper execute(String paperId, UpdatePaperCommand command, Authentication authentication) {
        String companyId = support.companyId(authentication);
        Paper existing = paperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("PAPER_NOT_FOUND", "Papel no encontrado"));
        support.requireSameCompany(existing.getCompanyId(), companyId);
        String name = requireName(command.name());
        String unit = Paper.normalizeUnit(command.unit() == null ? existing.getUnit() : command.unit());
        var width = command.width() == null ? existing.getWidth() : command.width();
        var height = command.height() == null ? existing.getHeight() : command.height();
        if (paperRepository.existsDuplicateExcludingId(
                companyId, name, command.grammage(), width, height, unit, paperId)) {
            throw new PaperAlreadyExistsException(name);
        }
        Paper updated = existing.update(
                name,
                command.grammage(),
                width,
                height,
                unit,
                Objects.requireNonNullElse(command.coated(), existing.isCoated()),
                command.acceptsRemnants(),
                command.minRemnantWidth(),
                command.minRemnantHeight(),
                command.minRemnantUnit(),
                Objects.requireNonNullElse(command.state(), existing.isState()),
                LocalDateTime.now(clock)
        );
        return paperRepository.save(updated);
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        return name.trim();
    }
}
