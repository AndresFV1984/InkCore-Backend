package com.inkcore.application.station.usecase;

import com.inkcore.application.station.StationSupport;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.ports.out.StationOperationEventRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStationTraceReportUseCaseTest {

    private static final String COMPANY_ID = "company-1";
    private static final String ORDER_ID = "po-1";
    private static final String PROCESS_KEY = "preprensa";

    @Mock StationSupport support;
    @Mock StationOperationEventRepositoryPort eventRepository;
    @Mock ClientRepositoryPort clientRepository;
    @Mock ListStationOrderProcessesUseCase processesUseCase;
    @Mock Authentication authentication;

    private GetStationTraceReportUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetStationTraceReportUseCase(
                support, eventRepository, clientRepository, processesUseCase);
        when(support.companyId(authentication)).thenReturn(COMPANY_ID);
    }

    @Test
    void execute_includeTimelineFalse_returnsEmptyTimeline() {
        ProductionOrder order = order();
        when(support.requireOrder(ORDER_ID, COMPANY_ID)).thenReturn(order);
        when(clientRepository.findById("client-1")).thenReturn(Optional.empty());
        when(processesUseCase.executeInternal(ORDER_ID, COMPANY_ID)).thenReturn(List.of(processRow()));
        when(eventRepository.findAllByProductionOrderIdAndProcessKey(COMPANY_ID, ORDER_ID, PROCESS_KEY))
                .thenReturn(List.of(event("e1", StationEventType.AVANCE_UNIDADES)));

        GetStationTraceReportUseCase.TraceReport report = useCase.execute(
                null, null, null, ORDER_ID, null, null, false, authentication);

        assertEquals(1, report.rows().size());
        assertTrue(report.rows().get(0).timeline().isEmpty());
        assertEquals(10, report.rows().get(0).unidadesProcesadas());
    }

    @Test
    void execute_includeTimelineTrue_embedsTimeline() {
        ProductionOrder order = order();
        when(support.requireOrder(ORDER_ID, COMPANY_ID)).thenReturn(order);
        when(clientRepository.findById("client-1")).thenReturn(Optional.empty());
        when(processesUseCase.executeInternal(ORDER_ID, COMPANY_ID)).thenReturn(List.of(processRow()));
        when(eventRepository.findAllByProductionOrderIdAndProcessKey(COMPANY_ID, ORDER_ID, PROCESS_KEY))
                .thenReturn(List.of(event("e1", StationEventType.INICIO_FASE)));

        GetStationTraceReportUseCase.TraceReport report = useCase.execute(
                null, null, null, ORDER_ID, null, null, true, authentication);

        assertEquals(1, report.rows().get(0).timeline().size());
        assertEquals("e1", report.rows().get(0).timeline().get(0).id());
        assertEquals("inicio_fase", report.rows().get(0).timeline().get(0).type());
    }

    @Test
    void execute_nullIncludeTimeline_defaultsToTrue() {
        ProductionOrder order = order();
        when(support.requireOrder(ORDER_ID, COMPANY_ID)).thenReturn(order);
        when(clientRepository.findById(anyString())).thenReturn(Optional.empty());
        when(processesUseCase.executeInternal(ORDER_ID, COMPANY_ID)).thenReturn(List.of(processRow()));
        when(eventRepository.findAllByProductionOrderIdAndProcessKey(COMPANY_ID, ORDER_ID, PROCESS_KEY))
                .thenReturn(List.of(event("e1", StationEventType.PARO)));

        GetStationTraceReportUseCase.TraceReport report = useCase.execute(
                null, null, null, ORDER_ID, null, null, null, authentication);

        assertEquals(1, report.rows().get(0).timeline().size());
    }

    private static ProductionOrder order() {
        ProductionOrder order = ProductionOrder.reconstitute();
        order.setProductionOrderId(ORDER_ID);
        order.setCompanyId(COMPANY_ID);
        order.setClientId("client-1");
        return order;
    }

    private static ListStationOrderProcessesUseCase.StationProcessRow processRow() {
        return new ListStationOrderProcessesUseCase.StationProcessRow(
                "preprensa",
                PROCESS_KEY,
                null,
                null,
                "op-1",
                100,
                10,
                0,
                "IN_PROGRESS",
                1000L,
                0L
        );
    }

    private static StationOperationEvent event(String id, StationEventType type) {
        StationOperationEvent event = new StationOperationEvent();
        event.setEventId(id);
        event.setEventType(type);
        event.setProcessKey(PROCESS_KEY);
        event.setOccurredAt(LocalDateTime.of(2026, 9, 1, 10, 0));
        event.setUnits(5);
        return event;
    }
}
