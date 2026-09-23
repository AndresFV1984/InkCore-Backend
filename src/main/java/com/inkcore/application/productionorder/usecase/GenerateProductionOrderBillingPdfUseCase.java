package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.ports.out.ProductionOrderPdfPort;
import com.inkcore.domain.productionorder.service.ProductionOrderCalculator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class GenerateProductionOrderBillingPdfUseCase {

    private final ProductionOrderSupport support;
    private final ProductionOrderPdfPort pdfPort;
    private final ClientRepositoryPort clientRepository;

    public GenerateProductionOrderBillingPdfUseCase(
            ProductionOrderSupport support,
            ProductionOrderPdfPort pdfPort,
            ClientRepositoryPort clientRepository
    ) {
        this.support = support;
        this.pdfPort = pdfPort;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public byte[] execute(String productionOrderId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        ProductionOrder order = support.requireOrder(productionOrderId, companyId);
        String clientName = clientRepository.findById(order.getClientId())
                .map(c -> c.getName())
                .orElse(order.getClientId());
        BigDecimal total = ProductionOrderCalculator.calculateTotalToCharge(order);
        return pdfPort.generateBillingPdf(order, clientName, total == null ? BigDecimal.ZERO : total);
    }
}
