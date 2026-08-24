package com.inkcore.application.platetype.usecase;

import com.inkcore.domain.platetype.exception.PlateTypeAlreadyExistsException;
import com.inkcore.domain.platetype.model.PlateType;
import com.inkcore.domain.platetype.ports.out.PlateTypeRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

@Service
public class CreatePlateTypeUseCase {

    private final PlateTypeRepositoryPort plateTypeRepository;
    private final Clock clock;

    public CreatePlateTypeUseCase(PlateTypeRepositoryPort plateTypeRepository, Clock clock) {
        this.plateTypeRepository = plateTypeRepository;
        this.clock = clock;
    }

    @Transactional
    public PlateType execute(CreatePlateTypeCommand command) {
        String name = requireTrimmed(command.name(), "El nombre es obligatorio");
        if (command.value() == null) {
            throw new IllegalArgumentException("El valor es obligatorio");
        }
        if (plateTypeRepository.existsByCompanyIdAndNameIgnoreCase(command.companyId(), name)) {
            throw new PlateTypeAlreadyExistsException("name", name);
        }

        boolean state = Objects.requireNonNullElse(command.state(), true);
        String unit = command.unit() == null || command.unit().isBlank() ? "cm" : command.unit();

        PlateType plateType = PlateType.createNew(
                command.companyId(),
                name,
                command.width(),
                command.height(),
                unit,
                command.value(),
                state,
                LocalDate.now(clock)
        );
        return plateTypeRepository.save(plateType);
    }

    private static String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
