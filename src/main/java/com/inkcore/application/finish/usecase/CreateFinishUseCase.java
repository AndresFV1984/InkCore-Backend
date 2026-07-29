package com.inkcore.application.finish.usecase;

import com.inkcore.domain.finish.exception.FinishAlreadyExistsException;
import com.inkcore.domain.finish.model.Finish;
import com.inkcore.domain.finish.ports.out.FinishRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

@Service
public class CreateFinishUseCase {

    private final FinishRepositoryPort finishRepository;
    private final Clock clock;

    public CreateFinishUseCase(FinishRepositoryPort finishRepository, Clock clock) {
        this.finishRepository = finishRepository;
        this.clock = clock;
    }

    @Transactional
    public Finish execute(CreateFinishCommand command) {
        String name = requireTrimmed(command.name(), "El nombre es obligatorio");
        if (finishRepository.existsByCompanyIdAndNameIgnoreCase(command.companyId(), name)) {
            throw new FinishAlreadyExistsException("name", name);
        }

        boolean quickAccess = Objects.requireNonNullElse(command.quickAccess(), false);
        boolean state = Objects.requireNonNullElse(command.state(), true);

        Finish finish = Finish.createNew(
                command.companyId(),
                name,
                command.minCost(),
                command.valuePerCm2(),
                quickAccess,
                state,
                LocalDate.now(clock)
        );
        return finishRepository.save(finish);
    }

    private static String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
