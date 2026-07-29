package com.inkcore.application.finishingprocess.usecase;

import com.inkcore.domain.finishingprocess.exception.FinishingProcessAlreadyExistsException;
import com.inkcore.domain.finishingprocess.model.FinishingProcess;
import com.inkcore.domain.finishingprocess.ports.out.FinishingProcessRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateFinishingProcessUseCase {

    private final FinishingProcessRepositoryPort finishingProcessRepository;

    public UpdateFinishingProcessUseCase(FinishingProcessRepositoryPort finishingProcessRepository) {
        this.finishingProcessRepository = finishingProcessRepository;
    }

    @Transactional
    public FinishingProcess execute(UpdateFinishingProcessCommand command) {
        FinishingProcess existing = finishingProcessRepository.findById(command.finishingProcessId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FINISHING_PROCESS_NOT_FOUND",
                        "Proceso de acabado no encontrado"
                ));

        String name = requireTrimmed(command.name(), "El nombre es obligatorio");
        if (finishingProcessRepository.existsByCompanyIdAndNameIgnoreCaseExcludingFinishingProcessId(
                existing.getCompanyId(), name, existing.getFinishingProcessId())) {
            throw new FinishingProcessAlreadyExistsException("name", name);
        }

        FinishingProcess updated = existing.update(
                name,
                command.minCost(),
                command.valuePerCm2(),
                command.quickAccess(),
                command.state()
        );
        return finishingProcessRepository.save(updated);
    }

    private static String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
