package com.inkcore.application.station;

import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.model.StationOrderProgress;
import com.inkcore.domain.station.ports.out.StationOperationEventRepositoryPort;
import com.inkcore.domain.station.ports.out.StationOrderProgressRepositoryPort;
import com.inkcore.domain.station.service.StationAvailableQuantityCalculator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Persiste {@code cantidadDisponible} (MIN de unidades procesadas por proceso)
 * para el listado de OP / Pedidos. Fuente de verdad del botón Entregar.
 */
@Component
public class StationOrderProgressService {

    private final StationOrderProgressRepositoryPort orderProgressRepository;
    private final StationOperationEventRepositoryPort eventRepository;

    public StationOrderProgressService(
            StationOrderProgressRepositoryPort orderProgressRepository,
            StationOperationEventRepositoryPort eventRepository
    ) {
        this.orderProgressRepository = orderProgressRepository;
        this.eventRepository = eventRepository;
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

    /**
     * Recalcula cargando eventos de estación. Usar tras cambios de tercerización
     * o cierre admin que no pasan por un AVANCE_UNIDADES.
     */
    public void recalculateFromPersistedEvents(ProductionOrder order, LocalDateTime now) {
        if (order == null || order.getProductionOrderId() == null || order.getCompanyId() == null) {
            return;
        }
        List<StationOperationEvent> events = eventRepository.findAllByProductionOrderId(
                order.getCompanyId(),
                order.getProductionOrderId()
        );
        recalculate(order, events, now);
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
