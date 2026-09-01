package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStage;
import com.inkcore.domain.seller.ports.out.SellerRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateProductionOrderSpecificationsUseCase {

    private final ProductionOrderSupport support;
    private final ProductionOrderOperatorsApplier operatorsApplier;
    private final ClientRepositoryPort clientRepository;
    private final SellerRepositoryPort sellerRepository;

    public UpdateProductionOrderSpecificationsUseCase(
            ProductionOrderSupport support,
            ProductionOrderOperatorsApplier operatorsApplier,
            ClientRepositoryPort clientRepository,
            SellerRepositoryPort sellerRepository
    ) {
        this.support = support;
        this.operatorsApplier = operatorsApplier;
        this.clientRepository = clientRepository;
        this.sellerRepository = sellerRepository;
    }

    @Transactional
    public ProductionOrder execute(
            String productionOrderId,
            UpdateProductionOrderSpecificationsCommand command,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        ProductionOrder order = support.requireOrder(productionOrderId, companyId);
        support.assertVersion(order, command.version());

        requireClient(command.clientId(), companyId);
        if (command.sellerId() != null && !command.sellerId().isBlank()) {
            requireSeller(command.sellerId(), companyId);
        }

        order.updateSpecifications(
                command.clientId(),
                command.workName(),
                command.sellerId(),
                command.orderDate(),
                command.requestedQuantity(),
                command.proposalQuantity1(),
                command.proposalQuantity2(),
                support.now()
        );
        operatorsApplier.apply(
                order,
                companyId,
                command.operators(),
                command.operatorUserId(),
                ProductionOrderStage.PREPRESS
        );
        order.setUpdatedBy(userId);
        return support.repository().save(order);
    }

    private void requireClient(String clientId, String companyId) {
        var client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("CLIENT_NOT_FOUND", "Cliente no encontrado"));
        if (!companyId.equals(client.getCompanyId())) {
            throw new ResourceNotFoundException("CLIENT_NOT_FOUND", "Cliente no encontrado");
        }
    }

    private void requireSeller(String sellerId, String companyId) {
        var seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("SELLER_NOT_FOUND", "Vendedor no encontrado"));
        if (!companyId.equals(seller.getCompanyId())) {
            throw new ResourceNotFoundException("SELLER_NOT_FOUND", "Vendedor no encontrado");
        }
    }
}
