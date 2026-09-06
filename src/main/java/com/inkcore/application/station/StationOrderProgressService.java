package com.inkcore.application.station;

import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.model.StationOrderProgress;
import com.inkcore.domain.station.ports.out.StationOrderProgressRepositoryPort;
import com.inkcore.domain.station.service.StationAvailableQuantityCalculator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StationOrderProgressService {

    private final StationOrderProgressRepositoryPort orderProgressRepository;

    public StationOrderProgressService(StationOrderProgressRepositoryPort orderProgressRepository) {
        this.orderProgressRepository = orderProgressRepository;
    }

    public void recalculate(
            ProductionOrder order,
            List<StationOperationEvent> orderEvents,
            LocalDateTime now
    ) {
        if (order == null || order.getProductionOrderId() == null) {
            return;
        }
        int cantidad = StationAvailableQuantityCalculator.calculate(order, orderEvents);
        StationOrderProgress progress = orderProgressRepository
                .findByProductionOrderId(order.getCompanyId(), order.getProductionOrderId())
                .orElseGet(StationOrderProgress::new);
        progress.setCompanyId(order.getCompanyId());
        progress.setProductionOrderId(order.getProductionOrderId());
        progress.setCantidadDisponible(cantidad);
        progress.setUpdatedAt(now);
        orderProgressRepository.save(progress);
    }

    public int getCantidadDisponible(String companyId, String productionOrderId) {
        return orderProgressRepository.findByProductionOrderId(companyId, productionOrderId)
                .map(StationOrderProgress::getCantidadDisponible)
                .orElse(0);
    }

    public Map<String, Integer> mapCantidadDisponible(String companyId, Collection<String> productionOrderIds) {
        return orderProgressRepository.findByProductionOrderIds(companyId, productionOrderIds).stream()
                .collect(Collectors.toMap(
                        StationOrderProgress::getProductionOrderId,
                        StationOrderProgress::getCantidadDisponible,
                        (a, b) -> a
                ));
    }
}
