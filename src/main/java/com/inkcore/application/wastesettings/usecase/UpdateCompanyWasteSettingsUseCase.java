package com.inkcore.application.wastesettings.usecase;

import com.inkcore.application.machine.usecase.MachineSupport;
import com.inkcore.domain.wastesettings.model.CompanyWasteSettings;
import com.inkcore.domain.wastesettings.ports.out.CompanyWasteSettingsRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class UpdateCompanyWasteSettingsUseCase {

    private final CompanyWasteSettingsRepositoryPort repository;
    private final MachineSupport support;
    private final Clock clock;

    public UpdateCompanyWasteSettingsUseCase(
            CompanyWasteSettingsRepositoryPort repository,
            MachineSupport support,
            Clock clock
    ) {
        this.repository = repository;
        this.support = support;
        this.clock = clock;
    }

    @Transactional
    public CompanyWasteSettings execute(
            BigDecimal cutMin,
            BigDecimal cutMax,
            BigDecimal cutDefault,
            BigDecimal operationalMin,
            BigDecimal operationalMax,
            BigDecimal operationalDefault,
            BigDecimal prepressMin,
            BigDecimal prepressMax,
            BigDecimal prepressDefault,
            BigDecimal finishedMin,
            BigDecimal finishedMax,
            BigDecimal finishedDefault,
            BigDecimal finishingMin,
            BigDecimal finishingMax,
            BigDecimal finishingDefault,
            BigDecimal cutMakereadySheets,
            BigDecimal operationalMakereadySheets,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        CompanyWasteSettings current = repository.findByCompanyId(companyId)
                .orElseGet(() -> CompanyWasteSettings.initialSuggestion(companyId));
        return repository.save(current.update(
                cutMin, cutMax, cutDefault, operationalMin, operationalMax, operationalDefault,
                prepressMin, prepressMax, prepressDefault,
                finishedMin, finishedMax, finishedDefault,
                finishingMin, finishingMax, finishingDefault,
                cutMakereadySheets, operationalMakereadySheets,
                LocalDateTime.now(clock)
        ));
    }
}
