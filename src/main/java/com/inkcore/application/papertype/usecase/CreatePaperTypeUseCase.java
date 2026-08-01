package com.inkcore.application.papertype.usecase;

import com.inkcore.domain.cutlayout.ports.out.CutLayoutRepositoryPort;
import com.inkcore.domain.papertype.exception.PaperTypeAlreadyExistsException;
import com.inkcore.domain.papertype.model.PaperType;
import com.inkcore.domain.papertype.model.PaperTypeCutAssignment;
import com.inkcore.domain.papertype.ports.out.PaperTypeRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class CreatePaperTypeUseCase {

    private final PaperTypeRepositoryPort paperTypeRepository;
    private final CutLayoutRepositoryPort cutLayoutRepository;
    private final Clock clock;

    public CreatePaperTypeUseCase(
            PaperTypeRepositoryPort paperTypeRepository,
            CutLayoutRepositoryPort cutLayoutRepository,
            Clock clock
    ) {
        this.paperTypeRepository = paperTypeRepository;
        this.cutLayoutRepository = cutLayoutRepository;
        this.clock = clock;
    }

    @Transactional
    public PaperType execute(CreatePaperTypeCommand command) {
        String name = requireTrimmed(command.name(), "El nombre es obligatorio");
        if (command.packageUnit() == null) {
            throw new IllegalArgumentException("La unidad de empaque es obligatoria");
        }
        if (command.sheetValue() == null) {
            throw new IllegalArgumentException("El valor de la hoja es obligatorio");
        }
        if (paperTypeRepository.existsByCompanyIdAndNameIgnoreCase(command.companyId(), name)) {
            throw new PaperTypeAlreadyExistsException("name", name);
        }

        boolean coated = Objects.requireNonNullElse(command.coated(), false);
        boolean state = Objects.requireNonNullElse(command.state(), true);
        String unit = command.unit() == null || command.unit().isBlank() ? "cm" : command.unit();
        List<PaperTypeCutAssignment> assignments = PaperTypeCutAssignmentResolver.resolve(
                command.companyId().trim(),
                command.cutLayouts(),
                cutLayoutRepository
        );

        PaperType paperType = PaperType.createNew(
                command.companyId(),
                name,
                command.width(),
                command.height(),
                unit,
                command.sheetValue(),
                command.packageUnit(),
                coated,
                state,
                LocalDate.now(clock),
                assignments
        );
        return paperTypeRepository.save(paperType);
    }

    private static String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
