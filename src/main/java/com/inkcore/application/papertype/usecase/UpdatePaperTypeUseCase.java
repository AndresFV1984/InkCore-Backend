package com.inkcore.application.papertype.usecase;

import com.inkcore.domain.cutlayout.ports.out.CutLayoutRepositoryPort;
import com.inkcore.domain.papertype.exception.PaperTypeAlreadyExistsException;
import com.inkcore.domain.papertype.model.PaperType;
import com.inkcore.domain.papertype.model.PaperTypeCutAssignment;
import com.inkcore.domain.papertype.ports.out.PaperTypeRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UpdatePaperTypeUseCase {

    private final PaperTypeRepositoryPort paperTypeRepository;
    private final CutLayoutRepositoryPort cutLayoutRepository;

    public UpdatePaperTypeUseCase(
            PaperTypeRepositoryPort paperTypeRepository,
            CutLayoutRepositoryPort cutLayoutRepository
    ) {
        this.paperTypeRepository = paperTypeRepository;
        this.cutLayoutRepository = cutLayoutRepository;
    }

    @Transactional
    public PaperType execute(UpdatePaperTypeCommand command) {
        PaperType existing = paperTypeRepository.findById(command.paperTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PAPER_TYPE_NOT_FOUND",
                        "Tipo de papel no encontrado"
                ));

        String name = requireTrimmed(command.name(), "El nombre es obligatorio");
        if (paperTypeRepository.existsByCompanyIdAndNameIgnoreCaseExcludingPaperTypeId(
                existing.getCompanyId(), name, existing.getPaperTypeId())) {
            throw new PaperTypeAlreadyExistsException("name", name);
        }

        List<PaperTypeCutAssignment> assignments = PaperTypeCutAssignmentResolver.resolve(
                existing.getCompanyId(),
                command.cutLayouts(),
                cutLayoutRepository
        );

        PaperType updated = existing.update(
                name,
                command.width(),
                command.height(),
                command.unit(),
                command.sheetValue(),
                command.packageUnit(),
                command.coated(),
                command.state(),
                assignments
        );
        return paperTypeRepository.save(updated);
    }

    private static String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
