package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.bankaccount.ports.out.BankAccountRepositoryPort;
import com.inkcore.domain.productionorder.exception.ProductionOrderBusinessRuleException;
import com.inkcore.domain.productionorder.model.BillingDetails;
import com.inkcore.domain.productionorder.model.ClientCostingMode;
import com.inkcore.domain.productionorder.model.DiscountType;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStage;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class UpdateProductionOrderBillingUseCase {

    private final ProductionOrderSupport support;
    private final BankAccountRepositoryPort bankAccountRepository;

    public UpdateProductionOrderBillingUseCase(
            ProductionOrderSupport support,
            BankAccountRepositoryPort bankAccountRepository
    ) {
        this.support = support;
        this.bankAccountRepository = bankAccountRepository;
    }

    @Transactional
    public ProductionOrder execute(
            String productionOrderId,
            UpdateProductionOrderBillingCommand command,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        ProductionOrder order = support.requireOrder(productionOrderId, companyId);
        support.assertVersion(order, command.version());

        validate(command, companyId);

        BillingDetails billing = order.getBilling() == null ? new BillingDetails() : order.getBilling();
        billing.setProductionOrderId(order.getProductionOrderId());
        billing.setCompanyId(companyId);
        billing.setBillingDiscountType(DiscountType.fromValue(command.billingDiscountType()));
        billing.setBillingDiscountValue(command.billingDiscountValue());
        billing.setClientCostingMode(ClientCostingMode.fromValue(command.clientCostingMode()));
        billing.setClientDiscountType(DiscountType.fromValue(command.clientDiscountType()));
        billing.setClientDiscountValue(command.clientDiscountValue());
        billing.setClientProfitabilityType(DiscountType.fromValue(command.clientProfitabilityType()));
        billing.setClientProfitabilityValue(command.clientProfitabilityValue());
        billing.setClientVolumeCosting(command.clientVolumeCosting());
        billing.setDeliveryStartDate(command.deliveryStartDate());
        billing.setDeliveryEndDate(command.deliveryEndDate());
        billing.setAdvancePercentage(Objects.requireNonNullElse(command.advancePercentage(), new BigDecimal("50")));
        billing.setClientSignatureName(blankToNull(command.clientSignatureName()));
        billing.setBankAccountId(blankToNull(command.bankAccountId()));
        if (Boolean.TRUE.equals(command.completed())) {
            billing.setBillingCompletedAt(support.now());
        }

        order.setBilling(billing);
        order.upsertOperator(ProductionOrderStage.BILLING, command.operatorUserId());
        order.setUpdatedAt(support.now());
        order.setUpdatedBy(userId);
        return support.repository().save(order);
    }

    private void validate(UpdateProductionOrderBillingCommand command, String companyId) {
        List<String> errors = new ArrayList<>();
        if (command.clientCostingMode() != null && !command.clientCostingMode().isBlank()) {
            try {
                ClientCostingMode.fromValue(command.clientCostingMode());
            } catch (IllegalArgumentException ex) {
                errors.add("clientCostingMode: exact|volume");
            }
        }
        if (command.bankAccountId() != null && !command.bankAccountId().isBlank()) {
            var account = bankAccountRepository.findById(command.bankAccountId()).orElse(null);
            if (account == null || !companyId.equals(account.getCompanyId())) {
                throw new ResourceNotFoundException("BANK_ACCOUNT_NOT_FOUND", "Cuenta bancaria no encontrada");
            }
        }
        if (!errors.isEmpty()) {
            throw new ProductionOrderBusinessRuleException("Regla de negocio incumplida", errors);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
