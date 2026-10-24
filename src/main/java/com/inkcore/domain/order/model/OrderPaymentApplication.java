package com.inkcore.domain.order.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Aplicación de un LIQ a una CxC o saldo a favor del pedido (cxcDocumentId null). */
public final class OrderPaymentApplication {

    private String applicationId;
    private String companyId;
    private String paymentId;
    private String productionOrderId;
    private String customerOrderId;
    private String cxcDocumentId;
    private BigDecimal amount;
    private String originApplicationId;
    private PaymentApplicationStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime createdAt;

    public OrderPaymentApplication() {
        this.applicationId = UUID.randomUUID().toString();
        this.status = PaymentApplicationStatus.ACTIVO;
        this.amount = BigDecimal.ZERO;
    }

    public String getApplicationId() { return applicationId; }
    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getProductionOrderId() { return productionOrderId; }
    public void setProductionOrderId(String productionOrderId) { this.productionOrderId = productionOrderId; }
    public String getCustomerOrderId() { return customerOrderId; }
    public void setCustomerOrderId(String customerOrderId) { this.customerOrderId = customerOrderId; }
    public String getCxcDocumentId() { return cxcDocumentId; }
    public void setCxcDocumentId(String cxcDocumentId) { this.cxcDocumentId = cxcDocumentId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getOriginApplicationId() { return originApplicationId; }
    public void setOriginApplicationId(String originApplicationId) {
        this.originApplicationId = originApplicationId;
    }
    public PaymentApplicationStatus getStatus() { return status; }
    public void setStatus(PaymentApplicationStatus status) { this.status = status; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isCreditFavor() {
        return cxcDocumentId == null || cxcDocumentId.isBlank();
    }
}
