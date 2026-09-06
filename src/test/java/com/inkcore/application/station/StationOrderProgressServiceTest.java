package com.inkcore.application.station;

import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.model.StationOrderProgress;
import com.inkcore.domain.station.ports.out.StationOrderProgressRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StationOrderProgressServiceTest {

    @Mock StationOrderProgressRepositoryPort repository;

    private StationOrderProgressService service;

    @BeforeEach
    void setUp() {
        service = new StationOrderProgressService(repository);
    }

    @Test
    void recalculate_upsertsMinProcessedUnits() {
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setCompanyId("c1");
        order.setProductionOrderId("po-1");
        order.setRequestedQuantity(1000);
        order.setPostpressRecords(List.of());

        when(repository.findByProductionOrderId("c1", "po-1")).thenReturn(Optional.empty());

        List<StationOperationEvent> events = List.of(
                avance("preprensa", 1000),
                avance("corte-papel", 900),
                avance("impresion", 700)
        );
        LocalDateTime now = LocalDateTime.of(2026, 9, 2, 12, 0);

        service.recalculate(order, events, now);

        ArgumentCaptor<StationOrderProgress> captor = ArgumentCaptor.forClass(StationOrderProgress.class);
        verify(repository).save(captor.capture());
        assertEquals(700, captor.getValue().getCantidadDisponible());
        assertEquals("po-1", captor.getValue().getProductionOrderId());
        assertEquals(now, captor.getValue().getUpdatedAt());
    }

    private static StationOperationEvent avance(String processKey, int units) {
        StationOperationEvent event = new StationOperationEvent();
        event.setEventType(StationEventType.AVANCE_UNIDADES);
        event.setProcessKey(processKey);
        event.setUnits(units);
        event.setOccurredAt(LocalDateTime.now());
        return event;
    }
}
