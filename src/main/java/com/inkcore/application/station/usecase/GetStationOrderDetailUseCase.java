package com.inkcore.application.station.usecase;

import com.inkcore.application.station.StationSupport;
import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.station.service.StationValidationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetStationOrderDetailUseCase {

    private final StationSupport support;
    private final ClientRepositoryPort clientRepository;

    public GetStationOrderDetailUseCase(StationSupport support, ClientRepositoryPort clientRepository) {
        this.support = support;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public StationOrderDetail execute(String productionOrderId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        ProductionOrder order = support.requireOrder(productionOrderId, companyId);
        String clientName = clientRepository.findById(order.getClientId()).map(Client::getName).orElse(null);
        String designName = order.getPrepress() == null ? null : order.getPrepress().getDesignName();
        boolean executionAllowed = new StationValidationService().orderAllowsOperatorExecution(order);
        return new StationOrderDetail(
                order.getProductionOrderId(),
                order.getOrderNumber(),
                order.getWorkName(),
                order.getClientId(),
                clientName,
                designName,
                displayStatus(order.getStatus()),
                executionAllowed,
                order.getRequestedQuantity(),
                order.getOperators(),
                order.getPostpressRecords()
        );
    }

    private static String displayStatus(String status) {
        if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            return "En Proceso";
        }
        return status;
    }

    public record StationOrderDetail(
            String productionOrderId,
            String displayNumber,
            String workName,
            String clientId,
            String clientName,
            String designName,
            String productionStatus,
            boolean executionAllowed,
            int requestedQuantity,
            java.util.List<com.inkcore.domain.productionorder.model.OperatorAssignment> operators,
            java.util.List<com.inkcore.domain.productionorder.model.PostpressRecord> postpressRecords
    ) {
    }
}
