package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.exception.PaperAlreadyExistsException;
import com.inkcore.domain.paper.model.Paper;
import com.inkcore.domain.paper.ports.out.PaperRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class CreatePaperUseCase {

    private final PaperRepositoryPort paperRepository;
    private final PaperSupport support;
    private final Clock clock;

    public CreatePaperUseCase(PaperRepositoryPort paperRepository, PaperSupport support, Clock clock) {
        this.paperRepository = paperRepository;
        this.support = support;
        this.clock = clock;
    }

    @Transactional
    public Paper execute(CreatePaperCommand command, Authentication authentication) {
        String companyId = support.companyId(authentication);
        String name = requireName(command.name());
        String unit = Paper.normalizeUnit(command.unit());
        if (paperRepository.existsDuplicate(
                companyId, name, command.grammage(), command.width(), command.height(), unit)) {
            throw new PaperAlreadyExistsException(name);
        }
        Paper paper = Paper.createNew(
                companyId,
                name,
                command.grammage(),
                command.width(),
                command.height(),
                unit,
                Objects.requireNonNullElse(command.coated(), false),
                command.acceptsRemnants(),
                command.minRemnantWidth(),
                command.minRemnantHeight(),
                command.minRemnantUnit(),
                Objects.requireNonNullElse(command.state(), true),
                LocalDate.now(clock),
                LocalDateTime.now(clock)
        );
        return paperRepository.save(paper);
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        return name.trim();
    }
}
