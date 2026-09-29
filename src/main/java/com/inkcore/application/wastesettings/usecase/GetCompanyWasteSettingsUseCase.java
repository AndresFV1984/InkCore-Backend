package com.inkcore.application.wastesettings.usecase;

import com.inkcore.application.machine.usecase.MachineSupport;
import com.inkcore.domain.wastesettings.model.CompanyWasteSettings;
import com.inkcore.domain.wastesettings.ports.out.CompanyWasteSettingsRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCompanyWasteSettingsUseCase {

    private final CompanyWasteSettingsRepositoryPort repository;
    private final MachineSupport support;

    public GetCompanyWasteSettingsUseCase(CompanyWasteSettingsRepositoryPort repository, MachineSupport support) {
        this.repository = repository;
        this.support = support;
    }

    @Transactional(readOnly = true)
    public CompanyWasteSettings execute(Authentication authentication) {
        String companyId = support.companyId(authentication);
        return repository.findByCompanyId(companyId)
                .orElseGet(() -> CompanyWasteSettings.initialSuggestion(companyId));
    }
}
