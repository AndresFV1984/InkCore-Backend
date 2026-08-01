package com.inkcore.infrastructure.out.persistence.colorconversion.repository;

import com.inkcore.infrastructure.out.persistence.colorconversion.entity.ConversionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaConversionHistoryRepository extends JpaRepository<ConversionHistoryEntity, String> {
}
