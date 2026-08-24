package com.inkcore.application.assemblyprice.usecase;

import com.inkcore.domain.assemblyprice.exception.AssemblyPriceAlreadyExistsException;
import com.inkcore.domain.assemblyprice.model.AssemblyPrice;
import com.inkcore.domain.assemblyprice.ports.out.AssemblyPriceRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

@Service
public class CreateAssemblyPriceUseCase {

    private final AssemblyPriceRepositoryPort assemblyPriceRepository;
    private final Clock clock;

    public CreateAssemblyPriceUseCase(AssemblyPriceRepositoryPort assemblyPriceRepository, Clock clock) {
        this.assemblyPriceRepository = assemblyPriceRepository;
        this.clock = clock;
    }

    @Transactional
    public AssemblyPrice execute(CreateAssemblyPriceCommand command) {
        String name = requireTrimmed(command.name(), "El nombre es obligatorio");
        if (command.cost() == null) {
            throw new IllegalArgumentException("El costo es obligatorio");
        }
        if (assemblyPriceRepository.existsByCompanyIdAndNameIgnoreCase(command.companyId(), name)) {
            throw new AssemblyPriceAlreadyExistsException("name", name);
        }

        boolean state = Objects.requireNonNullElse(command.state(), true);

        AssemblyPrice assemblyPrice = AssemblyPrice.createNew(
                command.companyId(),
                name,
                command.cost(),
                state,
                LocalDate.now(clock)
        );
        return assemblyPriceRepository.save(assemblyPrice);
    }

    private static String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
