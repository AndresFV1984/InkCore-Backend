package com.inkcore.application.station.usecase;

import com.inkcore.application.station.StationSupport;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.station.model.StationEventFilter;
import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.ports.out.StationOperationEventRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStationProcessTimelineUseCaseTest {

    private static final String COMPANY_ID = "company-1";
    private static final String ORDER_ID = "po-1";
    private static final String PROCESS_KEY = "terminado:rec-barniz-001";

    @Mock StationSupport support;
    @Mock StationOperationEventRepositoryPort eventRepository;
    @Mock Authentication authentication;

    private GetStationProcessTimelineUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetStationProcessTimelineUseCase(support, eventRepository);
        when(support.companyId(authentication)).thenReturn(COMPANY_ID);
        when(support.requireOrder(ORDER_ID, COMPANY_ID)).thenReturn(null);
    }

    @Test
    void execute_buildsFilterAndReturnsPage() {
        StationOperationEvent event = sampleEvent();
        PageQuery query = PageQuery.of(0, 10);
        when(eventRepository.findPage(any(StationEventFilter.class), eq(query)))
                .thenReturn(new PageResult<>(List.of(event), 0, 10, 1));

        PageResult<StationOperationEvent> result = useCase.execute(
                ORDER_ID,
                "terminado%3Arec-barniz-001",
                "rec-barniz-001",
                "op-1",
                query,
                authentication
        );

        assertEquals(1, result.content().size());
        assertEquals(1, result.totalElements());
        assertEquals(false, result.hasNext());

        ArgumentCaptor<StationEventFilter> captor = ArgumentCaptor.forClass(StationEventFilter.class);
        verify(eventRepository).findPage(captor.capture(), eq(query));
        StationEventFilter filter = captor.getValue();
        assertEquals(COMPANY_ID, filter.companyId());
        assertEquals(ORDER_ID, filter.productionOrderId());
        assertEquals(PROCESS_KEY, filter.processKey());
        assertEquals("rec-barniz-001", filter.catalogItemId());
        assertEquals("op-1", filter.userId());
    }

    @Test
    void execute_sizeAboveMax_clampedViaPageQuery() {
        PageQuery query = PageQuery.of(0, 150);
        when(eventRepository.findPage(any(StationEventFilter.class), eq(query)))
                .thenReturn(new PageResult<>(List.of(), 0, 100, 0));

        PageResult<StationOperationEvent> result = useCase.execute(
                ORDER_ID, PROCESS_KEY, null, null, query, authentication);

        assertEquals(100, result.size());
        assertTrue(result.content().isEmpty());
    }

    private static StationOperationEvent sampleEvent() {
        StationOperationEvent event = new StationOperationEvent();
        event.setEventId("evt-1");
        event.setProductionOrderId(ORDER_ID);
        event.setProcessKey(PROCESS_KEY);
        event.setCatalogItemId("rec-barniz-001");
        event.setEventType(StationEventType.AVANCE_UNIDADES);
        event.setOccurredAt(LocalDateTime.of(2026, 9, 1, 10, 15));
        event.setUnits(250);
        event.setUserId("op-1");
        return event;
    }
}
