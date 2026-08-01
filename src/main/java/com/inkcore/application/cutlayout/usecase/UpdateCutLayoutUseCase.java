package com.inkcore.application.cutlayout.usecase;

import com.inkcore.domain.cutlayout.exception.CutLayoutAlreadyExistsException;
import com.inkcore.domain.cutlayout.model.CutLayout;
import com.inkcore.domain.cutlayout.ports.out.CutLayoutRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateCutLayoutUseCase {

    private final CutLayoutRepositoryPort cutLayoutRepository;

    public UpdateCutLayoutUseCase(CutLayoutRepositoryPort cutLayoutRepository) {
        this.cutLayoutRepository = cutLayoutRepository;
    }

    @Transactional
    public CutLayout execute(UpdateCutLayoutCommand command) {
        CutLayout existing = cutLayoutRepository.findById(command.cutLayoutId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CUT_LAYOUT_NOT_FOUND",
                        "Despiece no encontrado"
                ));

        String name = requireTrimmed(command.name(), "El nombre es obligatorio");
        if (cutLayoutRepository.existsByCompanyIdAndNameIgnoreCaseExcludingCutLayoutId(
                existing.getCompanyId(), name, existing.getCutLayoutId())) {
            throw new CutLayoutAlreadyExistsException("name", name);
        }

        CutLayout updated = existing.update(
                name,
                command.width(),
                command.height(),
                command.unit(),
                command.piecesPerSheet(),
                command.state()
        );
        return cutLayoutRepository.save(updated);
    }

    private static String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
