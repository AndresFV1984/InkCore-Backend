package com.inkcore.infrastructure.out.persistence.inkestimation.repository;

import com.inkcore.infrastructure.out.persistence.inkestimation.entity.InkEstimateHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaInkEstimateHistoryRepository extends JpaRepository<InkEstimateHistoryEntity, String> {
}
