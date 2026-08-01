package com.inkcore.application.papertype.usecase;

import com.inkcore.domain.cutlayout.model.CutLayout;
import com.inkcore.domain.cutlayout.ports.out.CutLayoutRepositoryPort;
import com.inkcore.domain.papertype.model.PaperTypeCutAssignment;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.List;

final class PaperTypeCutAssignmentResolver {

    private PaperTypeCutAssignmentResolver() {
    }

    static List<PaperTypeCutAssignment> resolve(
            String companyId,
            List<PaperTypeCutAssignmentCommand> commands,
            CutLayoutRepositoryPort cutLayoutRepository
    ) {
        if (commands == null || commands.isEmpty()) {
            return List.of();
        }
        List<PaperTypeCutAssignment> resolved = new ArrayList<>();
        for (PaperTypeCutAssignmentCommand command : commands) {
            if (command == null || command.cutLayoutId() == null || command.cutLayoutId().isBlank()) {
                throw new IllegalArgumentException("El despiece es obligatorio en las asignaciones");
            }
            CutLayout cutLayout = cutLayoutRepository.findById(command.cutLayoutId().trim())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "CUT_LAYOUT_NOT_FOUND",
                            "Despiece no encontrado: " + command.cutLayoutId()
                    ));
            if (!cutLayout.getCompanyId().equals(companyId)) {
                throw new IllegalArgumentException(
                        "El despiece no pertenece a la misma empresa: " + command.cutLayoutId()
                );
            }
            resolved.add(PaperTypeCutAssignment.of(cutLayout.getCutLayoutId(), command.cutValue()));
        }
        return resolved;
    }
}
