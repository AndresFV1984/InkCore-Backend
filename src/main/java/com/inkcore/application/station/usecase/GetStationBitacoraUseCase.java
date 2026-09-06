package com.inkcore.application.station.usecase;

import com.inkcore.application.station.StationIntervalService;
import com.inkcore.application.station.StationSupport;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.model.StationOperationInterval;
import com.inkcore.domain.station.ports.out.StationOperationEventRepositoryPort;
import com.inkcore.domain.station.ports.out.StationOperationIntervalRepositoryPort;
import com.inkcore.domain.station.service.StationValidationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class GetStationBitacoraUseCase {

    private final StationSupport support;
    private final StationOperationEventRepositoryPort eventRepository;
    private final StationOperationIntervalRepositoryPort intervalRepository;
    private final StationIntervalService intervalService;

    public GetStationBitacoraUseCase(
            StationSupport support,
            StationOperationEventRepositoryPort eventRepository,
            StationOperationIntervalRepositoryPort intervalRepository,
            StationIntervalService intervalService
    ) {
        this.support = support;
        this.eventRepository = eventRepository;
        this.intervalRepository = intervalRepository;
        this.intervalService = intervalService;
    }

    @Transactional(readOnly = true)
    public BitacoraSummary execute(
            String productionOrderId,
            String processKeyEncoded,
            String catalogItemIdFilter,
            PageQuery pageQuery,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        support.requireOrder(productionOrderId, companyId);
        String processKey = URLDecoder.decode(processKeyEncoded, StandardCharsets.UTF_8);
        PageQuery query = pageQuery == null ? PageQuery.of(0, 10) : pageQuery;
        String itemFilter = blankToNull(catalogItemIdFilter);

        List<StationOperationEvent> events = eventRepository
                .findAllByProductionOrderIdAndProcessKey(companyId, productionOrderId, processKey)
                .stream()
                .filter(event -> matchesCatalogItem(event.getCatalogItemId(), event.getProcessKey(), itemFilter))
                .toList();
        List<StationOperationInterval> intervals = intervalRepository
                .findByProductionOrderIdAndProcessKey(companyId, productionOrderId, processKey)
                .stream()
                .filter(interval -> matchesCatalogItem(interval.getCatalogItemId(), interval.getProcessKey(), itemFilter))
                .toList();

        String catalogItemId = itemFilter != null
                ? itemFilter
                : events.stream()
                .map(StationOperationEvent::getCatalogItemId)
                .filter(id -> id != null && !id.isBlank())
                .findFirst()
                .orElse(null);
        String catalogItemLabel = events.stream()
                .map(StationOperationEvent::getCatalogItemLabel)
                .filter(label -> label != null && !label.isBlank())
                .findFirst()
                .orElse(null);

        List<BitacoraEntry> entries = new ArrayList<>();
        List<BitacoraPause> pauses = new ArrayList<>();
        int pauseCount = 0;
        for (StationOperationEvent event : events) {
            if (event.getEventType() == StationEventType.AVANCE_UNIDADES
                    || event.getEventType() == StationEventType.ENTREGA_PARCIAL
                    || event.getEventType() == StationEventType.ENTREGA_TOTAL) {
                entries.add(new BitacoraEntry(
                        event.getEventId(),
                        event.getEventType().getDbValue(),
                        event.getOccurredAt().toString(),
                        event.getUnits(),
                        event.getNote()
                ));
            }
            if (event.getEventType() == StationEventType.PARO) {
                pauseCount++;
                pauses.add(new BitacoraPause(
                        event.getEventId(),
                        event.getPauseReason(),
                        event.getOccurredAt().toString(),
                        event.getNote()
                ));
            }
        }

        entries.sort(Comparator.comparing(BitacoraEntry::at).reversed());

        int from = Math.min(query.offset(), entries.size());
        int to = Math.min(from + query.size(), entries.size());
        List<BitacoraEntry> pageEntries = List.copyOf(entries.subList(from, to));
        PageResult<BitacoraEntry> page = new PageResult<>(pageEntries, query.page(), query.size(), entries.size());

        boolean pausedNow = events.stream()
                .map(StationOperationEvent::getUserId)
                .filter(id -> id != null && !id.isBlank())
                .findFirst()
                .map(userId -> StationValidationService.hasOpenPause(events, processKey, userId, catalogItemId))
                .orElse(false);

        return new BitacoraSummary(
                processKey,
                catalogItemId,
                catalogItemLabel,
                intervalService.sumLaborMs(intervals),
                intervalService.sumPausedMs(intervals),
                pauseCount,
                pausedNow,
                pageEntries,
                pauses,
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages(),
                page.hasNext()
        );
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    /**
     * Si no hay filtro, todo el processKey. Si hay filtro: coincide por catalogItemId;
     * alias null solo cuando el proceso no es de ítem (terminado/acabado).
     */
    static boolean matchesCatalogItem(String eventCatalogItemId, String processKey, String filter) {
        if (filter == null) {
            return true;
        }
        if (filter.equals(eventCatalogItemId)) {
            return true;
        }
        if (eventCatalogItemId != null && !eventCatalogItemId.isBlank()) {
            return false;
        }
        return processKey == null
                || (!processKey.startsWith("terminado:") && !processKey.startsWith("acabado:"));
    }

    public record BitacoraSummary(
            String processKey,
            String catalogItemId,
            String catalogItemLabel,
            long laborTimeMs,
            long pausedTimeMs,
            int pauseCount,
            boolean isPausedNow,
            List<BitacoraEntry> entries,
            List<BitacoraPause> pauses,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {
    }

    public record BitacoraEntry(
            String id,
            String type,
            String at,
            Integer units,
            String note
    ) {
    }

    public record BitacoraPause(
            String id,
            String reason,
            String at,
            String note
    ) {
    }
}
