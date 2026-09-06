package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.order.model.ArSummary;
import com.inkcore.domain.order.model.OrderDelivery;
import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.domain.order.ports.out.ArSummaryRepositoryPort;
import com.inkcore.domain.order.ports.out.OrderDeliveryRepositoryPort;
import com.inkcore.domain.order.ports.out.OrderPaymentRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetArSummaryDetailUseCase {

    private final OrderSupport support;
    private final ArSummaryRepositoryPort arSummaryRepository;
    private final OrderDeliveryRepositoryPort deliveryRepository;
    private final OrderPaymentRepositoryPort paymentRepository;
    private final ClientRepositoryPort clientRepository;

    public GetArSummaryDetailUseCase(
            OrderSupport support,
            ArSummaryRepositoryPort arSummaryRepository,
            OrderDeliveryRepositoryPort deliveryRepository,
            OrderPaymentRepositoryPort paymentRepository,
            ClientRepositoryPort clientRepository
    ) {
        this.support = support;
        this.arSummaryRepository = arSummaryRepository;
        this.deliveryRepository = deliveryRepository;
        this.paymentRepository = paymentRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public ArDetail execute(String productionOrderId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        ProductionOrder order = support.requireActiveOrder(productionOrderId, companyId);

        ArSummary summary = arSummaryRepository.findByProductionOrderId(companyId, productionOrderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AR_SUMMARY_NOT_FOUND",
                        "No hay cuentas por cobrar para esta orden"
                ));

        String clientName = clientRepository.findById(summary.getClientId())
                .map(Client::getName)
                .orElse(null);

        ListArSummaryUseCase.ArSummaryRow row = new ListArSummaryUseCase.ArSummaryRow(
                summary.getProductionOrderId(),
                order.getOrderNumber(),
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

        List<OrderDelivery> deliveries = deliveryRepository.findByProductionOrderId(companyId, productionOrderId);
        List<OrderPayment> payments = paymentRepository.findByProductionOrderId(companyId, productionOrderId);
        return new ArDetail(row, deliveries, payments);
    }

    public record ArDetail(
            ListArSummaryUseCase.ArSummaryRow summary,
            List<OrderDelivery> deliveries,
            List<OrderPayment> payments
    ) {
    }
}
