package com.inkcore.domain.station.service;

import com.inkcore.domain.productionorder.model.OutsourcedFulfillmentStatus;
import com.inkcore.domain.productionorder.model.OutsourcedStage;
import com.inkcore.domain.productionorder.model.PostpressLine;
import com.inkcore.domain.productionorder.model.PostpressRecord;
import com.inkcore.domain.productionorder.model.PostpressType;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStage;
import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.domain.station.model.StationOperationEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StationAvailableQuantityCalculatorTest {

    @Test
    void calculate_exampleFromSpec_returnsMinIncludingTerminadosPhaseMin() {
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setRequestedQuantity(2000);
        order.setPostpressRecords(List.of(finishedProducts(
                line("rec-barniz-001", "Barniz UV"),
                line("rec-troquel-001", "Troquel")
        )));

        List<StationOperationEvent> events = new ArrayList<>();
        events.add(avance("preprensa", null, 2000));
        events.add(avance("corte-papel", null, 1500));
        events.add(avance("impresion", null, 800));
        events.add(avance("terminado:rec-barniz-001", "rec-barniz-001", 800));
        events.add(avance("terminado:rec-troquel-001", "rec-troquel-001", 500));

        assertEquals(500, StationAvailableQuantityCalculator.calculate(order, events));
    }

    @Test
    void calculate_withoutPostpress_onlyBaseProcesses() {
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setRequestedQuantity(1000);
        order.setPostpressRecords(List.of());

        List<StationOperationEvent> events = List.of(
                avance("preprensa", null, 1000),
                avance("corte-papel", null, 1000),
                avance("impresion", null, 400)
        );

        assertEquals(400, StationAvailableQuantityCalculator.calculate(order, events));
    }

    @Test
    void calculate_anyProcessZero_returnsZero() {
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setRequestedQuantity(1000);
        order.setPostpressRecords(List.of());

        List<StationOperationEvent> events = List.of(
                avance("preprensa", null, 1000),
                avance("corte-papel", null, 0),
                avance("impresion", null, 500)
        );

        assertEquals(0, StationAvailableQuantityCalculator.calculate(order, events));
    }

    @Test
    void calculate_noEvents_returnsZero() {
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setRequestedQuantity(1000);
        order.setPostpressRecords(List.of());

        assertEquals(0, StationAvailableQuantityCalculator.calculate(order, List.of()));
    }

    @Test
    void calculate_doesNotSubtractDeliveries() {
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setRequestedQuantity(1000);
        order.setPostpressRecords(List.of());

        StationOperationEvent delivery = new StationOperationEvent();
        delivery.setEventType(StationEventType.ENTREGA_PARCIAL);
        delivery.setProcessKey("impresion");
        delivery.setUnits(200);
        delivery.setOccurredAt(LocalDateTime.now());

        List<StationOperationEvent> events = List.of(
                avance("preprensa", null, 1000),
                avance("corte-papel", null, 1000),
                avance("impresion", null, 800),
                delivery
        );

        assertEquals(800, StationAvailableQuantityCalculator.calculate(order, events));
    }

    @Test
    void calculate_outsourcedCompletedCountsAsRequestedQuantity() {
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setCompanyId("c1");
        order.setProductionOrderId("po-1");
        order.setRequestedQuantity(2000);
        order.setPostpressRecords(List.of());
        order.setOutsourcedStages(List.of(
                outsourced(ProductionOrderStage.PREPRESS, OutsourcedFulfillmentStatus.COMPLETED)
        ));

        List<StationOperationEvent> events = List.of(
                // Preprensa tercerizada COMPLETED: sin AVANCE, aporta 2000
                avance("corte-papel", null, 2000),
                avance("impresion", null, 1500)
        );

        assertEquals(1500, StationAvailableQuantityCalculator.calculate(order, events));
    }

    @Test
    void calculate_outsourcedPendingBlocksAvailability() {
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setCompanyId("c1");
        order.setProductionOrderId("po-1");
        order.setRequestedQuantity(2000);
        order.setPostpressRecords(List.of());
        order.setOutsourcedStages(List.of(
                outsourced(ProductionOrderStage.PRINTING, OutsourcedFulfillmentStatus.DISPATCHED)
        ));

        List<StationOperationEvent> events = List.of(
                avance("preprensa", null, 2000),
                avance("corte-papel", null, 2000)
        );

        assertEquals(0, StationAvailableQuantityCalculator.calculate(order, events));
    }

    @Test
    void calculate_allStagesOutsourcedCompleted_returnsRequestedQuantity() {
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setCompanyId("c1");
        order.setProductionOrderId("po-1");
        order.setRequestedQuantity(1200);
        order.setPostpressRecords(List.of());
        order.setOutsourcedStages(List.of(
                outsourced(ProductionOrderStage.PREPRESS, OutsourcedFulfillmentStatus.COMPLETED),
                outsourced(ProductionOrderStage.CUTTING, OutsourcedFulfillmentStatus.COMPLETED),
                outsourced(ProductionOrderStage.PRINTING, OutsourcedFulfillmentStatus.COMPLETED)
        ));

        assertEquals(1200, StationAvailableQuantityCalculator.calculate(order, List.of()));
    }

    private static OutsourcedStage outsourced(
            ProductionOrderStage stage,
            OutsourcedFulfillmentStatus status
    ) {
        return OutsourcedStage.of(
                "c1",
                "po-1",
                stage,
                true,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                null,
                "sup-1",
                "Proveedor",
                LocalDate.of(2026, 1, 1),
                1200,
                null,
                null,
                null,
                LocalDate.of(2026, 1, 2),
                "envío",
                LocalDate.of(2026, 1, 10),
                status == OutsourcedFulfillmentStatus.COMPLETED ? LocalDate.of(2026, 1, 11) : null,
                status == OutsourcedFulfillmentStatus.COMPLETED ? "user-1" : null,
                status
        );
    }

    private static PostpressRecord finishedProducts(PostpressLine... lines) {
        PostpressRecord record = new PostpressRecord();
        record.setType(PostpressType.FINISHED_PRODUCT);
        record.setLines(List.of(lines));
        return record;
    }

    private static PostpressLine line(String catalogItemId, String name) {
        PostpressLine line = new PostpressLine();
        line.setCatalogItemId(catalogItemId);
        line.setItemName(name);
        return line;
    }

    private static StationOperationEvent avance(String processKey, String catalogItemId, int units) {
        StationOperationEvent event = new StationOperationEvent();
        event.setEventType(StationEventType.AVANCE_UNIDADES);
        event.setProcessKey(processKey);
        event.setCatalogItemId(catalogItemId);
        event.setUnits(units);
        event.setOccurredAt(LocalDateTime.of(2026, 9, 1, 10, 0));
        return event;
    }
}
