package com.inkcore.application.finishingprocess.usecase;

import com.inkcore.domain.finishingprocess.exception.FinishingProcessAlreadyExistsException;
import com.inkcore.domain.finishingprocess.model.FinishingProcess;
import com.inkcore.domain.finishingprocess.ports.out.FinishingProcessRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

@Service
public class CreateFinishingProcessUseCase {

    private final FinishingProcessRepositoryPort finishingProcessRepository;
    private final Clock clock;

    public CreateFinishingProcessUseCase(FinishingProcessRepositoryPort finishingProcessRepository, Clock clock) {
        this.finishingProcessRepository = finishingProcessRepository;
        this.clock = clock;
    }

    @Transactional
    public FinishingProcess execute(CreateFinishingProcessCommand command) {
        String name = requireTrimmed(command.name(), "El nombre es obligatorio");
        if (finishingProcessRepository.existsByCompanyIdAndNameIgnoreCase(command.companyId(), name)) {
            throw new FinishingProcessAlreadyExistsException("name", name);
        }

        boolean quickAccess = Objects.requireNonNullElse(command.quickAccess(), false);
        boolean state = Objects.requireNonNullElse(command.state(), true);

        FinishingProcess process = FinishingProcess.createNew(
                command.companyId(),
                name,
                command.minCost(),
                command.valuePerCm2(),
                quickAccess,
                state,
                LocalDate.now(clock)
        );
        return finishingProcessRepository.save(process);
    }

    private static String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
