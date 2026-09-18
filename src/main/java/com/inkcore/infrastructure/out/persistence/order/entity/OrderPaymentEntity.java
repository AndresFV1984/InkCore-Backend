package com.inkcore.infrastructure.out.persistence.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_payments", schema = "indicolors")
public class OrderPaymentEntity implements Persistable<String> {

    @Id
    @Column(name = "order_payment_id", length = 64)
    private String orderPaymentId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "payment_number", nullable = false, length = 32)
    private String paymentNumber;

    @Column(name = "production_order_id", nullable = false, length = 64)
    private String productionOrderId;

    @Column(name = "client_id", nullable = false, length = 64)
    private String clientId;

    @Column(name = "payment_type", nullable = false, length = 16)
    private String paymentType;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", nullable = false, length = 32)
    private String paymentMethod;

    @Column(length = 100)
    private String reference;

    @Column(name = "reversed_payment_id", length = 64)
    private String reversedPaymentId;

    @Column(name = "withholding_type", length = 32)
    private String withholdingType;

    @Column(name = "withholding_base", precision = 14, scale = 2)
    private BigDecimal withholdingBase;

    @Column(name = "withholding_rate", precision = 8, scale = 4)
    private BigDecimal withholdingRate;

    @Column(name = "certificate_ref", length = 100)
    private String certificateRef;

    @Column(name = "invoice_id", length = 64)
    private String invoiceId;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Column(name = "registered_by", nullable = false, length = 64)
    private String registeredBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Override
    public String getId() { return orderPaymentId; }

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    @PostPersist
    void markNotNew() { this.isNew = false; }

    public void markNew() { this.isNew = true; }

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
    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getReversedPaymentId() { return reversedPaymentId; }
    public void setReversedPaymentId(String reversedPaymentId) { this.reversedPaymentId = reversedPaymentId; }
    public String getWithholdingType() { return withholdingType; }
    public void setWithholdingType(String withholdingType) { this.withholdingType = withholdingType; }
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
