package com.inkcore.infrastructure.out.persistence.order.repository;

import com.inkcore.infrastructure.out.persistence.order.entity.ArSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface JpaArSummaryRepository
        extends JpaRepository<ArSummaryEntity, String>, JpaSpecificationExecutor<ArSummaryEntity> {

    Optional<ArSummaryEntity> findByCompanyIdAndProductionOrderId(String companyId, String productionOrderId);
}
