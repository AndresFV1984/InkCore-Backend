package com.inkcore.domain.order.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class OrderPayment {

    private String orderPaymentId;
    private String companyId;
    private String productionOrderId;
    private String clientId;
    private PaymentType paymentType;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String reference;
    private String reversedPaymentId;
    private LocalDateTime paidAt;
    private String registeredBy;
    private String notes;
    private LocalDateTime createdAt;

    public OrderPayment() {
        this.orderPaymentId = UUID.randomUUID().toString();
        this.paymentType = PaymentType.ABONO;
        this.amount = BigDecimal.ZERO;
    }

    public String getOrderPaymentId() { return orderPaymentId; }
    public void setOrderPaymentId(String orderPaymentId) { this.orderPaymentId = orderPaymentId; }
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getProductionOrderId() { return productionOrderId; }
    public void setProductionOrderId(String productionOrderId) { this.productionOrderId = productionOrderId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public PaymentType getPaymentType() { return paymentType; }
    public void setPaymentType(PaymentType paymentType) { this.paymentType = paymentType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getReversedPaymentId() { return reversedPaymentId; }
    public void setReversedPaymentId(String reversedPaymentId) { this.reversedPaymentId = reversedPaymentId; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public String getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(String registeredBy) { this.registeredBy = registeredBy; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
