package com.inkcore.application.cutlayout.usecase;

import com.inkcore.domain.cutlayout.exception.CutLayoutAlreadyExistsException;
import com.inkcore.domain.cutlayout.model.CutLayout;
import com.inkcore.domain.cutlayout.ports.out.CutLayoutRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

@Service
public class CreateCutLayoutUseCase {

    private final CutLayoutRepositoryPort cutLayoutRepository;
    private final Clock clock;

    public CreateCutLayoutUseCase(CutLayoutRepositoryPort cutLayoutRepository, Clock clock) {
        this.cutLayoutRepository = cutLayoutRepository;
        this.clock = clock;
    }

    @Transactional
    public CutLayout execute(CreateCutLayoutCommand command) {
        String name = requireTrimmed(command.name(), "El nombre es obligatorio");
        if (command.piecesPerSheet() == null) {
            throw new IllegalArgumentException("Las piezas por pliego son obligatorias");
        }
        if (cutLayoutRepository.existsByCompanyIdAndNameIgnoreCase(command.companyId(), name)) {
            throw new CutLayoutAlreadyExistsException("name", name);
        }

        boolean state = Objects.requireNonNullElse(command.state(), true);
        String unit = command.unit() == null || command.unit().isBlank() ? "cm" : command.unit();

        CutLayout cutLayout = CutLayout.createNew(
                command.companyId(),
                name,
                command.width(),
                command.height(),
                unit,
                command.piecesPerSheet(),
                state,
                LocalDate.now(clock)
        );
        return cutLayoutRepository.save(cutLayout);
    }

    private static String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
