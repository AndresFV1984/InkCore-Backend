package com.inkcore.application.platetype.usecase;

import com.inkcore.domain.platetype.exception.PlateTypeAlreadyExistsException;
import com.inkcore.domain.platetype.model.PlateType;
import com.inkcore.domain.platetype.ports.out.PlateTypeRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdatePlateTypeUseCase {

    private final PlateTypeRepositoryPort plateTypeRepository;

    public UpdatePlateTypeUseCase(PlateTypeRepositoryPort plateTypeRepository) {
        this.plateTypeRepository = plateTypeRepository;
    }

    @Transactional
    public PlateType execute(UpdatePlateTypeCommand command) {
        PlateType existing = plateTypeRepository.findById(command.plateTypeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PLATE_TYPE_NOT_FOUND",
                        "Tipo de plancha no encontrado"
                ));

        String name = requireTrimmed(command.name(), "El nombre es obligatorio");
        if (command.value() == null) {
            throw new IllegalArgumentException("El valor es obligatorio");
        }
        if (plateTypeRepository.existsByCompanyIdAndNameIgnoreCaseExcludingPlateTypeId(
                existing.getCompanyId(), name, existing.getPlateTypeId())) {
            throw new PlateTypeAlreadyExistsException("name", name);
        }

        PlateType updated = existing.update(
                name,
                command.width(),
                command.height(),
                command.unit(),
                command.value(),
                command.state()
        );
        return plateTypeRepository.save(updated);
    }

    private static String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
