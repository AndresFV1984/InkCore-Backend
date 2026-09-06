package com.inkcore.application.station.usecase;

import com.inkcore.application.station.StationSupport;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.station.model.StationEventFilter;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.ports.out.StationOperationEventRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Timeline rico de una operación (todos los tipos de evento), paginado en BD.
 * Orden: {@code occurredAt DESC}.
 */
@Service
public class GetStationProcessTimelineUseCase {

    private final StationSupport support;
    private final StationOperationEventRepositoryPort eventRepository;

    public GetStationProcessTimelineUseCase(
            StationSupport support,
            StationOperationEventRepositoryPort eventRepository
    ) {
        this.support = support;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<StationOperationEvent> execute(
            String productionOrderId,
            String processKeyEncoded,
            String catalogItemId,
            String userId,
            PageQuery pageQuery,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        support.requireOrder(productionOrderId, companyId);
        String processKey = URLDecoder.decode(processKeyEncoded, StandardCharsets.UTF_8);
        PageQuery query = pageQuery == null ? PageQuery.of(0, 10) : pageQuery;

        StationEventFilter filter = new StationEventFilter(
                companyId,
                productionOrderId.trim(),
                blankToNull(userId),
                null,
                processKey,
                blankToNull(catalogItemId),
                null,
                null,
                null
        );
        return eventRepository.findPage(filter, query);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
