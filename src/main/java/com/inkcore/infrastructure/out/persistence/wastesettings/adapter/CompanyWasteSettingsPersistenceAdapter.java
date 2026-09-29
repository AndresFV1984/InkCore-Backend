package com.inkcore.infrastructure.out.persistence.wastesettings.adapter;

import com.inkcore.domain.wastesettings.model.CompanyWasteSettings;
import com.inkcore.domain.wastesettings.ports.out.CompanyWasteSettingsRepositoryPort;
import com.inkcore.infrastructure.out.persistence.wastesettings.entity.CompanyWasteSettingsEntity;
import com.inkcore.infrastructure.out.persistence.wastesettings.repository.JpaCompanyWasteSettingsRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class CompanyWasteSettingsPersistenceAdapter implements CompanyWasteSettingsRepositoryPort {

    private final JpaCompanyWasteSettingsRepository repository;

    public CompanyWasteSettingsPersistenceAdapter(JpaCompanyWasteSettingsRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompanyWasteSettings> findByCompanyId(String companyId) {
        return repository.findById(companyId).map(this::toDomain);
    }

    @Override
    @Transactional
    public CompanyWasteSettings save(CompanyWasteSettings settings) {
        CompanyWasteSettingsEntity entity = repository.findById(settings.getCompanyId())
                .orElseGet(CompanyWasteSettingsEntity::new);
        copy(settings, entity);
        return toDomain(repository.save(entity));
    }

    private void copy(CompanyWasteSettings settings, CompanyWasteSettingsEntity entity) {
        entity.setCompanyId(settings.getCompanyId());
        entity.setCutWasteMinPercentage(settings.getCutWasteMinPercentage());
        entity.setCutWasteMaxPercentage(settings.getCutWasteMaxPercentage());
        entity.setCutWasteDefaultPercentage(settings.getCutWasteDefaultPercentage());
        entity.setOperationalWasteMinPercentage(settings.getOperationalWasteMinPercentage());
        entity.setOperationalWasteMaxPercentage(settings.getOperationalWasteMaxPercentage());
        entity.setOperationalWasteDefaultPercentage(settings.getOperationalWasteDefaultPercentage());
        entity.setPrepressWasteMinPercentage(settings.getPrepressWasteMinPercentage());
        entity.setPrepressWasteMaxPercentage(settings.getPrepressWasteMaxPercentage());
        entity.setPrepressWasteDefaultPercentage(settings.getPrepressWasteDefaultPercentage());
        entity.setFinishedWasteMinPercentage(settings.getFinishedWasteMinPercentage());
        entity.setFinishedWasteMaxPercentage(settings.getFinishedWasteMaxPercentage());
        entity.setFinishedWasteDefaultPercentage(settings.getFinishedWasteDefaultPercentage());
        entity.setFinishingWasteMinPercentage(settings.getFinishingWasteMinPercentage());
        entity.setFinishingWasteMaxPercentage(settings.getFinishingWasteMaxPercentage());
        entity.setFinishingWasteDefaultPercentage(settings.getFinishingWasteDefaultPercentage());
        entity.setCutMakereadySheets(settings.getCutMakereadySheets());
        entity.setOperationalMakereadySheets(settings.getOperationalMakereadySheets());
        entity.setUpdatedAt(settings.getUpdatedAt());
    }

    private CompanyWasteSettings toDomain(CompanyWasteSettingsEntity entity) {
        return CompanyWasteSettings.reconstitute(
                entity.getCompanyId(),
                entity.getCutWasteMinPercentage(),
                entity.getCutWasteMaxPercentage(),
                entity.getCutWasteDefaultPercentage(),
                entity.getOperationalWasteMinPercentage(),
                entity.getOperationalWasteMaxPercentage(),
                entity.getOperationalWasteDefaultPercentage(),
                entity.getPrepressWasteMinPercentage(),
                entity.getPrepressWasteMaxPercentage(),
                entity.getPrepressWasteDefaultPercentage(),
                entity.getFinishedWasteMinPercentage(),
                entity.getFinishedWasteMaxPercentage(),
                entity.getFinishedWasteDefaultPercentage(),
                entity.getFinishingWasteMinPercentage(),
                entity.getFinishingWasteMaxPercentage(),
                entity.getFinishingWasteDefaultPercentage(),
                entity.getCutMakereadySheets(),
                entity.getOperationalMakereadySheets(),
                entity.getUpdatedAt()
        );
    }
}
