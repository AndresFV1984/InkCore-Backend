package com.inkcore.application.station.usecase;

import com.inkcore.application.station.StationSupport;
import com.inkcore.application.station.StationOrderProgressService;
import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.station.model.StationProcessProgress;
import com.inkcore.domain.station.model.StationProcessStatus;
import com.inkcore.domain.station.ports.out.StationOperatorQueryPort;
import com.inkcore.domain.station.ports.out.StationProcessProgressRepositoryPort;
import com.inkcore.domain.station.service.StationValidationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class ListStationInboxUseCase {

    private final StationSupport support;
    private final StationOperatorQueryPort operatorQuery;
    private final StationProcessProgressRepositoryPort progressRepository;
    private final StationOrderProgressService orderProgressService;
    private final ClientRepositoryPort clientRepository;

    public ListStationInboxUseCase(
            StationSupport support,
            StationOperatorQueryPort operatorQuery,
            StationProcessProgressRepositoryPort progressRepository,
            StationOrderProgressService orderProgressService,
            ClientRepositoryPort clientRepository
    ) {
        this.support = support;
        this.operatorQuery = operatorQuery;
        this.progressRepository = progressRepository;
        this.orderProgressService = orderProgressService;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<StationInboxItem> execute(
            String productionStatus,
            String clientId,
            String processStatus,
            PageQuery pageQuery,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        List<String> orderIds = operatorQuery.findProductionOrderIdsByOperator(companyId, userId);
        if (orderIds.isEmpty()) {
            return new PageResult<>(List.of(), pageQuery.page(), pageQuery.size(), 0);
        }

        List<StationInboxItem> items = new ArrayList<>();
        Map<String, Integer> cantidadByOrder =
                orderProgressService.mapCantidadDisponible(companyId, orderIds);
        for (String orderId : orderIds) {
            ProductionOrder order = support.productionOrderRepository().findSummaryById(orderId).orElse(null);
            if (order == null || !order.isState()) {
                continue;
            }
            if (clientId != null && !clientId.isBlank() && !clientId.equals(order.getClientId())) {
                continue;
            }
            if (productionStatus != null && !productionStatus.isBlank()
                    && !matchesProductionStatus(order.getStatus(), productionStatus)) {
                continue;
            }

            List<StationProcessProgress> progresses =
                    progressRepository.findByProductionOrderId(companyId, order.getProductionOrderId())
                            .stream()
                            .filter(progress -> userId.equals(progress.getUserId()))
                            .toList();

            if (processStatus != null && !processStatus.isBlank() && !"all".equalsIgnoreCase(processStatus)) {
                boolean anyMatch = progresses.stream()
                        .anyMatch(progress -> progress.getStatus().getDbValue().equalsIgnoreCase(processStatus));
                if (!anyMatch) {
                    continue;
                }
            }

            String clientName = clientRepository.findById(order.getClientId())
                    .map(Client::getName)
                    .orElse(null);
            String designName = support.productionOrderRepository().findById(orderId)
                    .map(o -> o.getPrepress() == null ? null : o.getPrepress().getDesignName())
                    .orElse(null);

            items.add(new StationInboxItem(
                    order.getProductionOrderId(),
                    order.getOrderNumber(),
                    order.getWorkName(),
                    order.getClientId(),
                    clientName,
                    designName,
                    displayStatus(order.getStatus()),
                    new StationValidationService().orderAllowsOperatorExecution(order),
                    summarize(progresses),
                    cantidadByOrder.getOrDefault(order.getProductionOrderId(), 0)
            ));
        }

        items.sort(Comparator.comparing(StationInboxItem::displayNumber).reversed());
        int from = Math.min(pageQuery.offset(), items.size());
        int to = Math.min(from + pageQuery.size(), items.size());
        return new PageResult<>(items.subList(from, to), pageQuery.page(), pageQuery.size(), items.size());
    }

    private static boolean matchesProductionStatus(String orderStatus, String filter) {
        if (orderStatus == null) {
            return false;
        }
        String normalizedFilter = filter.trim();
        if ("En Proceso".equalsIgnoreCase(normalizedFilter)) {
            return StationValidationService.isInProgressStatus(orderStatus);
        }
        return orderStatus.equalsIgnoreCase(normalizedFilter);
    }

    private static String displayStatus(String status) {
        if (status == null) {
            return null;
        }
        if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            return "En Proceso";
        }
        return status;
    }

    private static AssignedProcessSummary summarize(List<StationProcessProgress> progresses) {
        if (progresses.isEmpty()) {
            return new AssignedProcessSummary(0, 0, 0, 0);
        }
        int done = (int) progresses.stream().filter(p -> p.getStatus() == StationProcessStatus.TERMINADO).count();
        int active = (int) progresses.stream().filter(p -> p.getStatus() == StationProcessStatus.EN_PROCESO).count();
        int pending = (int) progresses.stream().filter(p -> p.getStatus() == StationProcessStatus.PENDIENTE).count();
        int totalUnits = progresses.stream().mapToInt(StationProcessProgress::getTotalUnits).sum();
        int completedUnits = progresses.stream().mapToInt(StationProcessProgress::getCompletedUnits).sum();
        int progressPct = totalUnits <= 0 ? 0 : Math.min(100, (completedUnits * 100) / totalUnits);
        return new AssignedProcessSummary(done, active, pending, progressPct);
    }

    public record StationInboxItem(
            String productionOrderId,
            String displayNumber,
            String workName,
            String clientId,
            String clientName,
            String designName,
            String productionStatus,
            boolean executionAllowed,
            AssignedProcessSummary assignedProcessSummary,
            int cantidadDisponible
    ) {
    }

    public record AssignedProcessSummary(int done, int active, int pending, int progressPct) {
    }
}
