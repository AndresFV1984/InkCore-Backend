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
@Table(name = "order_payment_applications", schema = "indicolors")
public class OrderPaymentApplicationEntity implements Persistable<String> {

    @Id
    @Column(name = "application_id", length = 64)
    private String applicationId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "payment_id", nullable = false, length = 64)
    private String paymentId;

    @Column(name = "production_order_id", nullable = false, length = 64)
    private String productionOrderId;

    @Column(name = "customer_order_id", length = 64)
    private String customerOrderId;

    @Column(name = "cxc_document_id", length = 64)
    private String cxcDocumentId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "origin_application_id", length = 64)
    private String originApplicationId;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Override
    public String getId() { return applicationId; }

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    @PostPersist
    void markNotNew() { this.isNew = false; }

    public void markNew() { this.isNew = true; }

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
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
