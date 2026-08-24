package com.inkcore.domain.productionorder.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Detalle de Cobro, 1:1 con la orden
 * ({@code indicolors.production_order_billing_details}).
 */
public final class BillingDetails {

    private String productionOrderId;
    private String companyId;

    private DiscountType billingDiscountType;
    private BigDecimal billingDiscountValue;
    private ClientCostingMode clientCostingMode;
    private DiscountType clientDiscountType;
    private BigDecimal clientDiscountValue;
    private DiscountType clientProfitabilityType;
    private BigDecimal clientProfitabilityValue;
    private List<Map<String, Object>> clientVolumeCosting;
    private LocalDate deliveryStartDate;
    private LocalDate deliveryEndDate;
    private BigDecimal advancePercentage;
    private String clientSignatureName;
    private String bankAccountId;
    private LocalDateTime billingCompletedAt;

    public BillingDetails() {
    }

    public String getProductionOrderId() {
        return productionOrderId;
    }

    public void setProductionOrderId(String productionOrderId) {
        this.productionOrderId = productionOrderId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public DiscountType getBillingDiscountType() {
        return billingDiscountType;
    }

    public void setBillingDiscountType(DiscountType billingDiscountType) {
        this.billingDiscountType = billingDiscountType;
    }

    public BigDecimal getBillingDiscountValue() {
        return billingDiscountValue;
    }

    public void setBillingDiscountValue(BigDecimal billingDiscountValue) {
        this.billingDiscountValue = billingDiscountValue;
    }

    public ClientCostingMode getClientCostingMode() {
        return clientCostingMode;
    }

    public void setClientCostingMode(ClientCostingMode clientCostingMode) {
        this.clientCostingMode = clientCostingMode;
    }

    public DiscountType getClientDiscountType() {
        return clientDiscountType;
    }

    public void setClientDiscountType(DiscountType clientDiscountType) {
        this.clientDiscountType = clientDiscountType;
    }

    public BigDecimal getClientDiscountValue() {
        return clientDiscountValue;
    }

    public void setClientDiscountValue(BigDecimal clientDiscountValue) {
        this.clientDiscountValue = clientDiscountValue;
    }

    public DiscountType getClientProfitabilityType() {
        return clientProfitabilityType;
    }

    public void setClientProfitabilityType(DiscountType clientProfitabilityType) {
        this.clientProfitabilityType = clientProfitabilityType;
    }

    public BigDecimal getClientProfitabilityValue() {
        return clientProfitabilityValue;
    }

    public void setClientProfitabilityValue(BigDecimal clientProfitabilityValue) {
        this.clientProfitabilityValue = clientProfitabilityValue;
    }

    public List<Map<String, Object>> getClientVolumeCosting() {
        return clientVolumeCosting;
    }

    public void setClientVolumeCosting(List<Map<String, Object>> clientVolumeCosting) {
        if (clientVolumeCosting == null) {
            this.clientVolumeCosting = null;
            return;
        }
        List<Map<String, Object>> copy = new ArrayList<>();
        for (Map<String, Object> proposal : clientVolumeCosting) {
            copy.add(proposal == null ? new LinkedHashMap<>() : new LinkedHashMap<>(proposal));
        }
        this.clientVolumeCosting = copy;
    }

    public LocalDate getDeliveryStartDate() {
        return deliveryStartDate;
    }

    public void setDeliveryStartDate(LocalDate deliveryStartDate) {
        this.deliveryStartDate = deliveryStartDate;
    }

    public LocalDate getDeliveryEndDate() {
        return deliveryEndDate;
    }

    public void setDeliveryEndDate(LocalDate deliveryEndDate) {
        this.deliveryEndDate = deliveryEndDate;
    }

    public BigDecimal getAdvancePercentage() {
        return advancePercentage;
    }

    public void setAdvancePercentage(BigDecimal advancePercentage) {
        this.advancePercentage = advancePercentage;
    }

    public String getClientSignatureName() {
        return clientSignatureName;
    }

    public void setClientSignatureName(String clientSignatureName) {
        this.clientSignatureName = clientSignatureName;
    }

    public String getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(String bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public LocalDateTime getBillingCompletedAt() {
        return billingCompletedAt;
    }

    public void setBillingCompletedAt(LocalDateTime billingCompletedAt) {
        this.billingCompletedAt = billingCompletedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BillingDetails that)) {
            return false;
        }
        return Objects.equals(productionOrderId, that.productionOrderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productionOrderId);
    }
}
