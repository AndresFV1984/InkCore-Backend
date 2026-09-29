package com.inkcore.domain.wastesettings.ports.out;

import com.inkcore.domain.wastesettings.model.CompanyWasteSettings;

import java.util.Optional;

public interface CompanyWasteSettingsRepositoryPort {

    Optional<CompanyWasteSettings> findByCompanyId(String companyId);

    CompanyWasteSettings save(CompanyWasteSettings settings);
}
