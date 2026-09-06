package com.inkcore.application.station.usecase;

import com.inkcore.application.station.StationSupport;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.station.model.StationEventFilter;
import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.ports.out.StationOperationEventRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ListStationEventsUseCase {

    private final StationSupport support;
    private final StationOperationEventRepositoryPort eventRepository;

    public ListStationEventsUseCase(
            StationSupport support,
            StationOperationEventRepositoryPort eventRepository
    ) {
        this.support = support;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<StationOperationEvent> execute(
            String productionOrderId,
            String userId,
            String phase,
            String processKey,
            String eventType,
            LocalDateTime from,
            LocalDateTime to,
            PageQuery pageQuery,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        StationEventType parsedType = eventType == null || eventType.isBlank()
                ? null
                : StationEventType.fromValue(eventType);
        StationEventFilter filter = new StationEventFilter(
                companyId,
                blankToNull(productionOrderId),
                blankToNull(userId),
                blankToNull(phase),
                blankToNull(processKey),
                null,
                parsedType,
                from,
                to
        );
        return eventRepository.findPage(filter, pageQuery);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
