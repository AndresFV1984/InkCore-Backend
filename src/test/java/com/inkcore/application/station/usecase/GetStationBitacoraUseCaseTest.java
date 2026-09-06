package com.inkcore.application.station.usecase;

import com.inkcore.application.station.StationIntervalService;
import com.inkcore.application.station.StationSupport;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.model.StationOperationInterval;
import com.inkcore.domain.station.ports.out.StationOperationEventRepositoryPort;
import com.inkcore.domain.station.ports.out.StationOperationIntervalRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetStationBitacoraUseCaseTest {

    private static final String COMPANY_ID = "company-1";
    private static final String ORDER_ID = "po-1";
    private static final String PROCESS_KEY = "terminado:rec-barniz-001";

    @Mock StationSupport support;
    @Mock StationOperationEventRepositoryPort eventRepository;
    @Mock StationOperationIntervalRepositoryPort intervalRepository;
    @Mock StationIntervalService intervalService;
    @Mock Authentication authentication;

    private GetStationBitacoraUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetStationBitacoraUseCase(support, eventRepository, intervalRepository, intervalService);
        lenient().when(support.companyId(authentication)).thenReturn(COMPANY_ID);
        lenient().when(support.requireOrder(ORDER_ID, COMPANY_ID)).thenReturn(null);
        lenient().when(intervalRepository.findByProductionOrderIdAndProcessKey(COMPANY_ID, ORDER_ID, PROCESS_KEY))
                .thenReturn(List.of());
        lenient().when(intervalService.sumLaborMs(anyList())).thenReturn(7200000L);
        lenient().when(intervalService.sumPausedMs(anyList())).thenReturn(900000L);
    }

    @Test
    void execute_noEvents_emptyPage() {
        when(eventRepository.findAllByProductionOrderIdAndProcessKey(COMPANY_ID, ORDER_ID, PROCESS_KEY))
                .thenReturn(List.of());

        GetStationBitacoraUseCase.BitacoraSummary result = useCase.execute(
                ORDER_ID, PROCESS_KEY, null, PageQuery.of(0, 10), authentication);

        assertEquals(0, result.entries().size());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());
        assertFalse(result.hasNext());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(7200000L, result.laborTimeMs());
    }

    @Test
    void execute_25Entries_page0Has10AndHasNext() {
        when(eventRepository.findAllByProductionOrderIdAndProcessKey(COMPANY_ID, ORDER_ID, PROCESS_KEY))
                .thenReturn(avanceEvents(25, "rec-barniz-001"));

        GetStationBitacoraUseCase.BitacoraSummary result = useCase.execute(
                ORDER_ID, PROCESS_KEY, null, PageQuery.of(0, 10), authentication);

        assertEquals(10, result.entries().size());
        assertEquals(25, result.totalElements());
        assertEquals(3, result.totalPages());
        assertTrue(result.hasNext());
        assertEquals("2026-09-01T10:24", result.entries().get(0).at());
        assertEquals("2026-09-01T10:15", result.entries().get(9).at());
        assertEquals(1, result.pauses().size());
        assertEquals(1, result.pauseCount());
    }

    @Test
    void execute_25Entries_page2HasLast5() {
        when(eventRepository.findAllByProductionOrderIdAndProcessKey(COMPANY_ID, ORDER_ID, PROCESS_KEY))
                .thenReturn(avanceEvents(25, "rec-barniz-001"));

        GetStationBitacoraUseCase.BitacoraSummary result = useCase.execute(
                ORDER_ID, PROCESS_KEY, null, PageQuery.of(2, 10), authentication);

        assertEquals(5, result.entries().size());
        assertEquals(25, result.totalElements());
        assertEquals(3, result.totalPages());
        assertFalse(result.hasNext());
        assertEquals("2026-09-01T10:04", result.entries().get(0).at());
        assertEquals("2026-09-01T10:00", result.entries().get(4).at());
    }

    @Test
    void execute_outOfRangePage_emptyEntriesWithTotals() {
        when(eventRepository.findAllByProductionOrderIdAndProcessKey(COMPANY_ID, ORDER_ID, PROCESS_KEY))
                .thenReturn(avanceEvents(5, "rec-barniz-001"));

        GetStationBitacoraUseCase.BitacoraSummary result = useCase.execute(
                ORDER_ID, PROCESS_KEY, null, PageQuery.of(9, 10), authentication);

        assertTrue(result.entries().isEmpty());
        assertEquals(5, result.totalElements());
        assertEquals(1, result.totalPages());
        assertFalse(result.hasNext());
    }

    @Test
    void execute_catalogItemId_filtersEntriesAndPauses() {
        List<StationOperationEvent> events = new ArrayList<>();
        events.add(event("e1", StationEventType.AVANCE_UNIDADES, "rec-barniz-001", LocalDateTime.of(2026, 9, 1, 10, 0), 10));
        events.add(event("e2", StationEventType.AVANCE_UNIDADES, "rec-otro-002", LocalDateTime.of(2026, 9, 1, 10, 1), 20));
        events.add(event("e3", StationEventType.PARO, "rec-barniz-001", LocalDateTime.of(2026, 9, 1, 10, 2), null));
        events.add(event("e4", StationEventType.PARO, "rec-otro-002", LocalDateTime.of(2026, 9, 1, 10, 3), null));
        when(eventRepository.findAllByProductionOrderIdAndProcessKey(COMPANY_ID, ORDER_ID, PROCESS_KEY))
                .thenReturn(events);

        StationOperationInterval matching = new StationOperationInterval();
        matching.setCatalogItemId("rec-barniz-001");
        matching.setProcessKey(PROCESS_KEY);
        StationOperationInterval other = new StationOperationInterval();
        other.setCatalogItemId("rec-otro-002");
        other.setProcessKey(PROCESS_KEY);
        when(intervalRepository.findByProductionOrderIdAndProcessKey(COMPANY_ID, ORDER_ID, PROCESS_KEY))
                .thenReturn(List.of(matching, other));
        when(intervalService.sumLaborMs(eq(List.of(matching)))).thenReturn(1000L);
        when(intervalService.sumPausedMs(eq(List.of(matching)))).thenReturn(200L);

        GetStationBitacoraUseCase.BitacoraSummary result = useCase.execute(
                ORDER_ID, PROCESS_KEY, "rec-barniz-001", PageQuery.of(0, 10), authentication);

        assertEquals(1, result.entries().size());
        assertEquals("e1", result.entries().get(0).id());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.pauseCount());
        assertEquals("e3", result.pauses().get(0).id());
        assertEquals("rec-barniz-001", result.catalogItemId());
        assertEquals(1000L, result.laborTimeMs());
        assertEquals(200L, result.pausedTimeMs());
    }

    @Test
    void execute_sizeAboveMax_isClampedTo100() {
        when(eventRepository.findAllByProductionOrderIdAndProcessKey(COMPANY_ID, ORDER_ID, PROCESS_KEY))
                .thenReturn(avanceEvents(5, "rec-barniz-001"));

        GetStationBitacoraUseCase.BitacoraSummary result = useCase.execute(
                ORDER_ID, PROCESS_KEY, null, PageQuery.of(0, 150), authentication);

        assertEquals(100, result.size());
        assertEquals(5, result.entries().size());
        assertEquals(1, result.totalPages());
    }

    @Test
    void matchesCatalogItem_nullFilter_acceptsAll() {
        assertTrue(GetStationBitacoraUseCase.matchesCatalogItem(null, "preprensa", null));
        assertTrue(GetStationBitacoraUseCase.matchesCatalogItem("x", PROCESS_KEY, null));
    }

    @Test
    void matchesCatalogItem_itemProcess_rejectsNullAlias() {
        assertFalse(GetStationBitacoraUseCase.matchesCatalogItem(null, PROCESS_KEY, "rec-barniz-001"));
        assertTrue(GetStationBitacoraUseCase.matchesCatalogItem("rec-barniz-001", PROCESS_KEY, "rec-barniz-001"));
    }

    private static List<StationOperationEvent> avanceEvents(int count, String catalogItemId) {
        List<StationOperationEvent> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            events.add(event(
                    "avance-" + i,
                    StationEventType.AVANCE_UNIDADES,
                    catalogItemId,
                    LocalDateTime.of(2026, 9, 1, 10, i),
                    i + 1
            ));
        }
        events.add(event(
                "paro-1",
                StationEventType.PARO,
                catalogItemId,
                LocalDateTime.of(2026, 9, 1, 9, 40),
                null
        ));
        return events;
    }

    private static StationOperationEvent event(
            String id,
            StationEventType type,
            String catalogItemId,
            LocalDateTime at,
            Integer units
    ) {
        StationOperationEvent event = new StationOperationEvent();
        event.setEventId(id);
        event.setEventType(type);
        event.setProcessKey(PROCESS_KEY);
        event.setCatalogItemId(catalogItemId);
        event.setCatalogItemLabel("Barniz UV");
        event.setOccurredAt(at);
        event.setUnits(units);
        event.setUserId("op-1");
        event.setPauseReason(type == StationEventType.PARO ? "falla_maquina" : null);
        event.setNote(type == StationEventType.PARO ? "Cuchilla" : null);
        return event;
    }
}
