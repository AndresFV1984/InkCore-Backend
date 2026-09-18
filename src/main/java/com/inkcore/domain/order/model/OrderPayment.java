package com.inkcore.domain.order.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class OrderPayment {

    private String orderPaymentId;
    private String companyId;
    private String paymentNumber;
    private String productionOrderId;
    private String clientId;
    private PaymentType paymentType;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String reference;
    private String reversedPaymentId;
    private WithholdingType withholdingType;
    private BigDecimal withholdingBase;
    private BigDecimal withholdingRate;
    private String certificateRef;
    private String invoiceId;
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
    public String getPaymentNumber() { return paymentNumber; }
    public void setPaymentNumber(String paymentNumber) { this.paymentNumber = paymentNumber; }
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
    public WithholdingType getWithholdingType() { return withholdingType; }
    public void setWithholdingType(WithholdingType withholdingType) { this.withholdingType = withholdingType; }
    public BigDecimal getWithholdingBase() { return withholdingBase; }
    public void setWithholdingBase(BigDecimal withholdingBase) { this.withholdingBase = withholdingBase; }
    public BigDecimal getWithholdingRate() { return withholdingRate; }
    public void setWithholdingRate(BigDecimal withholdingRate) { this.withholdingRate = withholdingRate; }
    public String getCertificateRef() { return certificateRef; }
    public void setCertificateRef(String certificateRef) { this.certificateRef = certificateRef; }
    public String getInvoiceId() { return invoiceId; }
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public String getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(String registeredBy) { this.registeredBy = registeredBy; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
