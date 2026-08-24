package com.inkcore.application.platetype.usecase;

import com.inkcore.domain.platetype.model.PlateType;
import com.inkcore.domain.platetype.ports.out.PlateTypeRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPlateTypeByIdUseCase {

    private final PlateTypeRepositoryPort plateTypeRepository;

    public GetPlateTypeByIdUseCase(PlateTypeRepositoryPort plateTypeRepository) {
        this.plateTypeRepository = plateTypeRepository;
    }

    @Transactional(readOnly = true)
    public PlateType execute(String plateTypeId) {
        return plateTypeRepository.findById(plateTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PLATE_TYPE_NOT_FOUND",
                        "Tipo de plancha no encontrado"
                ));
    }
}
