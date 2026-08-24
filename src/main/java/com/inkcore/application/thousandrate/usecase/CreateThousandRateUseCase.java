package com.inkcore.application.thousandrate.usecase;

import com.inkcore.domain.thousandrate.exception.ThousandRateAlreadyExistsException;
import com.inkcore.domain.thousandrate.model.ThousandRate;
import com.inkcore.domain.thousandrate.ports.out.ThousandRateRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

@Service
public class CreateThousandRateUseCase {

    private final ThousandRateRepositoryPort thousandRateRepository;
    private final Clock clock;

    public CreateThousandRateUseCase(ThousandRateRepositoryPort thousandRateRepository, Clock clock) {
        this.thousandRateRepository = thousandRateRepository;
        this.clock = clock;
    }

    @Transactional
    public ThousandRate execute(CreateThousandRateCommand command) {
        String name = requireTrimmed(command.name(), "El nombre es obligatorio");
        String colorCategory = requireTrimmed(command.colorCategory(), "La categoría de color es obligatoria");
        if (command.price() == null) {
            throw new IllegalArgumentException("El precio es obligatorio");
        }
        if (thousandRateRepository.existsByCompanyIdAndNameIgnoreCase(command.companyId(), name)) {
            throw new ThousandRateAlreadyExistsException("name", name);
        }

        boolean state = Objects.requireNonNullElse(command.state(), true);
        boolean isDefault = Objects.requireNonNullElse(command.isDefault(), false);

        ThousandRate thousandRate = ThousandRate.createNew(
                command.companyId(),
                name,
                colorCategory,
                command.thousandUnit(),
                command.price(),
                state,
                command.minThresholdUnits(),
                command.minThousand(),
                command.decimalThreshold(),
                command.gripperFlipPrice(),
                command.squareFlipPrice(),
                isDefault,
                LocalDate.now(clock)
        );

        if (thousandRate.isDefault()) {
            thousandRateRepository.clearDefaultForCompanyAndColorCategoryExcept(
                    thousandRate.getCompanyId(),
                    thousandRate.getColorCategory(),
                    thousandRate.getThousandRateId()
            );
        }

        return thousandRateRepository.save(thousandRate);
    }

    private static String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
