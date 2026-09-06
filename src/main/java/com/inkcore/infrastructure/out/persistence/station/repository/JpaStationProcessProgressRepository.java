package com.inkcore.infrastructure.out.persistence.station.repository;

import com.inkcore.infrastructure.out.persistence.station.entity.StationProcessProgressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaStationProcessProgressRepository
        extends JpaRepository<StationProcessProgressEntity, StationProcessProgressEntity.IdKey> {

    List<StationProcessProgressEntity> findAllByCompanyIdAndProductionOrderId(
            String companyId,
            String productionOrderId
    );

    List<StationProcessProgressEntity> findAllByCompanyIdAndUserId(String companyId, String userId);
}
