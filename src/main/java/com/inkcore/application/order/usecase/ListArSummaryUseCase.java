package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.order.model.ArSummary;
import com.inkcore.domain.order.ports.out.ArSummaryRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ListArSummaryUseCase {

    private final OrderSupport support;
    private final ArSummaryRepositoryPort arSummaryRepository;
    private final ClientRepositoryPort clientRepository;

    public ListArSummaryUseCase(
            OrderSupport support,
            ArSummaryRepositoryPort arSummaryRepository,
            ClientRepositoryPort clientRepository
    ) {
        this.support = support;
        this.arSummaryRepository = arSummaryRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<ArSummaryRow> execute(
            String status,
            String clientId,
            String search,
            PageQuery pageQuery,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        PageQuery query = pageQuery == null ? PageQuery.of(0, PageQuery.DEFAULT_SIZE) : pageQuery;

        // Carga amplia si hay search (filtro en memoria por orderNumber/clientName).
        boolean hasSearch = search != null && !search.isBlank();
        PageQuery fetchQuery = hasSearch
                ? PageQuery.of(0, PageQuery.MAX_SIZE)
                : query;

        PageResult<ArSummary> page = arSummaryRepository.findPage(companyId, status, clientId, fetchQuery);
        List<ArSummaryRow> rows = new ArrayList<>();
        for (ArSummary summary : page.content()) {
            rows.add(toRow(summary));
        }

        if (hasSearch) {
            String needle = search.trim().toLowerCase(Locale.ROOT);
            rows = rows.stream()
                    .filter(row -> contains(row.orderNumber(), needle) || contains(row.clientName(), needle))
                    .toList();
            int from = Math.min(query.offset(), rows.size());
            int to = Math.min(from + query.size(), rows.size());
            return new PageResult<>(rows.subList(from, to), query.page(), query.size(), rows.size());
        }

        return new PageResult<>(rows, page.page(), page.size(), page.totalElements());
    }

    private ArSummaryRow toRow(ArSummary summary) {
        ProductionOrder order = support.productionOrderRepository()
                .findSummaryById(summary.getProductionOrderId())
                .orElse(null);
        String orderNumber = order == null ? null : order.getOrderNumber();
        String clientName = clientRepository.findById(summary.getClientId())
                .map(Client::getName)
                .orElse(null);
        return new ArSummaryRow(
                summary.getProductionOrderId(),
                orderNumber,
                summary.getClientId(),
                clientName,
                summary.getTotalUnits(),
                summary.getDeliveredUnits(),
                summary.getPendingUnits(),
                summary.getTotalOwed(),
                summary.getTotalPaid(),
                summary.getTotalRemaining(),
                summary.getStatus() == null ? null : summary.getStatus().getDbValue(),
                summary.getLastDeliveryAt(),
                summary.getLastPaymentAt()
        );
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    public record ArSummaryRow(
            String productionOrderId,
            String orderNumber,
            String clientId,
            String clientName,
            int totalUnits,
            int deliveredUnits,
            int pendingUnits,
            BigDecimal totalOwed,
            BigDecimal totalPaid,
            BigDecimal totalRemaining,
            String status,
            LocalDateTime lastDeliveryAt,
            LocalDateTime lastPaymentAt
    ) {
    }
}
