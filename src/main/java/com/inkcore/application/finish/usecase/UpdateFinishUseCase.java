package com.inkcore.application.finish.usecase;

import com.inkcore.domain.finish.exception.FinishAlreadyExistsException;
import com.inkcore.domain.finish.model.Finish;
import com.inkcore.domain.finish.ports.out.FinishRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateFinishUseCase {

    private final FinishRepositoryPort finishRepository;

    public UpdateFinishUseCase(FinishRepositoryPort finishRepository) {
        this.finishRepository = finishRepository;
    }

    @Transactional
    public Finish execute(UpdateFinishCommand command) {
        Finish existing = finishRepository.findById(command.finishId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FINISH_NOT_FOUND",
                        "Terminado no encontrado"
                ));

        String name = requireTrimmed(command.name(), "El nombre es obligatorio");
        if (finishRepository.existsByCompanyIdAndNameIgnoreCaseExcludingFinishId(
                existing.getCompanyId(), name, existing.getFinishId())) {
            throw new FinishAlreadyExistsException("name", name);
        }

        Finish updated = existing.update(
                name,
                command.minCost(),
                command.valuePerCm2(),
                command.quickAccess(),
                command.state()
        );
        return finishRepository.save(updated);
    }

    private static String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
