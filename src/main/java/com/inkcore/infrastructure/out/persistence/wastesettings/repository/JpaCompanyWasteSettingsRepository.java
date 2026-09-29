package com.inkcore.infrastructure.out.persistence.wastesettings.repository;

import com.inkcore.infrastructure.out.persistence.wastesettings.entity.CompanyWasteSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCompanyWasteSettingsRepository extends JpaRepository<CompanyWasteSettingsEntity, String> {
}
