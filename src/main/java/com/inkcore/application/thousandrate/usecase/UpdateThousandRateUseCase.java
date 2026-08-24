package com.inkcore.application.thousandrate.usecase;

import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import com.inkcore.domain.thousandrate.exception.ThousandRateAlreadyExistsException;
import com.inkcore.domain.thousandrate.model.ThousandRate;
import com.inkcore.domain.thousandrate.ports.out.ThousandRateRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateThousandRateUseCase {

    private final ThousandRateRepositoryPort thousandRateRepository;

    public UpdateThousandRateUseCase(ThousandRateRepositoryPort thousandRateRepository) {
        this.thousandRateRepository = thousandRateRepository;
    }

    @Transactional
    public ThousandRate execute(UpdateThousandRateCommand command) {
        ThousandRate existing = thousandRateRepository.findById(command.thousandRateId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "THOUSAND_RATE_NOT_FOUND",
                        "Tarifa por millar no encontrada"
                ));

        String name = requireTrimmed(command.name(), "El nombre es obligatorio");
        String colorCategory = requireTrimmed(command.colorCategory(), "La categoría de color es obligatoria");
        if (command.price() == null) {
            throw new IllegalArgumentException("El precio es obligatorio");
        }
        if (thousandRateRepository.existsByCompanyIdAndNameIgnoreCaseExcludingThousandRateId(
                existing.getCompanyId(), name, existing.getThousandRateId())) {
            throw new ThousandRateAlreadyExistsException("name", name);
        }

        ThousandRate updated = existing.update(
                name,
                colorCategory,
                command.thousandUnit(),
                command.price(),
                command.state(),
                command.minThresholdUnits(),
                command.minThousand(),
                command.decimalThreshold(),
                command.gripperFlipPrice(),
                command.squareFlipPrice(),
                command.isDefault()
        );

        if (updated.isDefault()) {
            thousandRateRepository.clearDefaultForCompanyAndColorCategoryExcept(
                    updated.getCompanyId(),
                    updated.getColorCategory(),
                    updated.getThousandRateId()
            );
        }

        return thousandRateRepository.save(updated);
    }

    private static String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
