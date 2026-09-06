package com.inkcore.infrastructure.out.persistence.station.repository;

import com.inkcore.infrastructure.out.persistence.station.entity.StationOrderProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaStationOrderProgressRepository extends JpaRepository<StationOrderProgressEntity, String> {

    Optional<StationOrderProgressEntity> findByCompanyIdAndProductionOrderId(String companyId, String productionOrderId);

    List<StationOrderProgressEntity> findAllByCompanyIdAndProductionOrderIdIn(
            String companyId,
            Collection<String> productionOrderIds
    );
}
