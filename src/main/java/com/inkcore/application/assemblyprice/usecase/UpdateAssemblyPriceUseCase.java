package com.inkcore.application.assemblyprice.usecase;

import com.inkcore.domain.assemblyprice.exception.AssemblyPriceAlreadyExistsException;
import com.inkcore.domain.assemblyprice.model.AssemblyPrice;
import com.inkcore.domain.assemblyprice.ports.out.AssemblyPriceRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateAssemblyPriceUseCase {

    private final AssemblyPriceRepositoryPort assemblyPriceRepository;

    public UpdateAssemblyPriceUseCase(AssemblyPriceRepositoryPort assemblyPriceRepository) {
        this.assemblyPriceRepository = assemblyPriceRepository;
    }

    @Transactional
    public AssemblyPrice execute(UpdateAssemblyPriceCommand command) {
        AssemblyPrice existing = assemblyPriceRepository.findById(command.assemblyPriceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ASSEMBLY_PRICE_NOT_FOUND",
                        "Precio de montaje no encontrado"
                ));

        String name = requireTrimmed(command.name(), "El nombre es obligatorio");
        if (command.cost() == null) {
            throw new IllegalArgumentException("El costo es obligatorio");
        }
        if (assemblyPriceRepository.existsByCompanyIdAndNameIgnoreCaseExcludingAssemblyPriceId(
                existing.getCompanyId(), name, existing.getAssemblyPriceId())) {
            throw new AssemblyPriceAlreadyExistsException("name", name);
        }

        AssemblyPrice updated = existing.update(name, command.cost(), command.state());
        return assemblyPriceRepository.save(updated);
    }

    private static String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
