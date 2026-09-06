package com.inkcore.application.station.usecase;

import com.inkcore.application.station.StationSupport;
import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import com.inkcore.domain.station.model.StationEventFilter;
import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.ports.out.StationOperationEventRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class GetStationTraceReportUseCase {

    private final StationSupport support;
    private final StationOperationEventRepositoryPort eventRepository;
    private final ClientRepositoryPort clientRepository;
    private final ListStationOrderProcessesUseCase processesUseCase;

    public GetStationTraceReportUseCase(
            StationSupport support,
            StationOperationEventRepositoryPort eventRepository,
            ClientRepositoryPort clientRepository,
            ListStationOrderProcessesUseCase processesUseCase
    ) {
        this.support = support;
        this.eventRepository = eventRepository;
        this.clientRepository = clientRepository;
        this.processesUseCase = processesUseCase;
    }

    @Transactional(readOnly = true)
    public TraceReport execute(
            String userId,
            LocalDate from,
            LocalDate to,
            String productionOrderId,
            String clientId,
            String processKey,
            Boolean includeTimeline,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        LocalDateTime fromDt = from == null ? null : from.atStartOfDay();
        LocalDateTime toDt = to == null ? null : to.atTime(LocalTime.MAX);
        boolean withTimeline = includeTimeline == null || includeTimeline;

        List<TraceRow> rows = new ArrayList<>();
        for (String orderId : resolveOrderIds(companyId, productionOrderId, userId, processKey, fromDt, toDt)) {
            ProductionOrder order;
            try {
                order = support.requireOrder(orderId, companyId);
            } catch (ResourceNotFoundException ex) {
                continue;
            }
            if (clientId != null && !clientId.isBlank() && !clientId.equals(order.getClientId())) {
                continue;
            }
            rows.addAll(buildRowsForOrder(order, userId, processKey, fromDt, toDt, withTimeline));
        }

        return new TraceReport(rows);
    }

    /**
     * Con OP explícita: solo esa. Sin OP: OPs distintas con eventos en estación
     * (filtrados por operario/proceso/rango), leídas desde BD paginadas.
     */
    private Set<String> resolveOrderIds(
            String companyId,
            String productionOrderId,
            String userId,
            String processKey,
            LocalDateTime from,
            LocalDateTime to
    ) {
        Set<String> orderIds = new LinkedHashSet<>();
        if (productionOrderId != null && !productionOrderId.isBlank()) {
            orderIds.add(productionOrderId.trim());
            return orderIds;
        }

        StationEventFilter filter = new StationEventFilter(
                companyId,
                null,
                blankToNull(userId),
                null,
                blankToNull(processKey),
                null,
                null,
                from,
                to
        );
        int page = 0;
        final int maxPages = 50;
        while (page < maxPages) {
            PageResult<StationOperationEvent> result =
                    eventRepository.findPage(filter, PageQuery.of(page, PageQuery.MAX_SIZE));
            for (StationOperationEvent event : result.content()) {
                String orderId = event.getProductionOrderId();
                if (orderId != null && !orderId.isBlank()) {
                    orderIds.add(orderId.trim());
                }
            }
            if (!result.hasNext()) {
                break;
            }
            page++;
        }
        return orderIds;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private List<TraceRow> buildRowsForOrder(
            ProductionOrder order,
            String userId,
            String processKeyFilter,
            LocalDateTime from,
            LocalDateTime to,
            boolean includeTimeline
    ) {
        String clientName = clientRepository.findById(order.getClientId()).map(Client::getName).orElse(null);
        List<ListStationOrderProcessesUseCase.StationProcessRow> processes =
                processesUseCase.executeInternal(order.getProductionOrderId(), order.getCompanyId());

        List<TraceRow> rows = new ArrayList<>();
        for (ListStationOrderProcessesUseCase.StationProcessRow process : processes) {
            if (processKeyFilter != null && !processKeyFilter.isBlank()
                    && !processKeyFilter.equals(process.processKey())) {
                continue;
            }
            if (userId != null && !userId.isBlank()
                    && process.userId() != null
                    && !userId.equals(process.userId())) {
                continue;
            }

            List<StationOperationEvent> events = eventRepository
                    .findAllByProductionOrderIdAndProcessKey(
                            order.getCompanyId(), order.getProductionOrderId(), process.processKey());
            if (from != null || to != null) {
                events = events.stream().filter(event -> {
                    if (from != null && event.getOccurredAt().isBefore(from)) {
                        return false;
                    }
                    if (to != null && event.getOccurredAt().isAfter(to)) {
                        return false;
                    }
                    return true;
                }).toList();
            }

            int partialDelivery = events.stream()
                    .filter(e -> e.getEventType() == StationEventType.ENTREGA_PARCIAL)
                    .map(StationOperationEvent::getUnits)
                    .filter(java.util.Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();

            List<TimelineEntry> timeline = includeTimeline
                    ? events.stream()
                    .map(e -> new TimelineEntry(
                            e.getEventId(),
                            e.getEventType().getDbValue(),
                            e.getOccurredAt().toString(),
                            e.getUnits(),
                            e.getPauseReason(),
                            e.getNote()
                    ))
                    .toList()
                    : List.of();

            rows.add(new TraceRow(
                    order.getProductionOrderId(),
                    order.getClientId(),
                    clientName,
                    process.phase(),
                    process.processKey(),
                    process.catalogItemId(),
                    process.catalogItemLabel(),
                    process.userId(),
                    process.completedUnits(),
                    partialDelivery,
                    process.laborTimeMs(),
                    process.pausedTimeMs(),
                    timeline
            ));
        }
        return rows;
    }

    public record TraceReport(List<TraceRow> rows) {
    }

    public record TraceRow(
            String orderId,
            String clientId,
            String clientName,
            String phase,
            String processKey,
            String catalogItemId,
            String catalogItemLabel,
            String userId,
            int unidadesProcesadas,
            int unidadesEntregaParcial,
            long tiempoLaboradoMs,
            long tiempoPausadoMs,
            List<TimelineEntry> timeline
    ) {
    }

    public record TimelineEntry(
            String id,
            String type,
            String at,
            Integer units,
            String pauseReason,
            String note
    ) {
    }
}
