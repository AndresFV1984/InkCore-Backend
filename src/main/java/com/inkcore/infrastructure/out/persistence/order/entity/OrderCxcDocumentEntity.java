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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_cxc_documents", schema = "indicolors")
public class OrderCxcDocumentEntity implements Persistable<String> {

    @Id
    @Column(name = "cxc_document_id", length = 64)
    private String cxcDocumentId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "cxc_number", nullable = false, length = 32)
    private String cxcNumber;

    @Column(name = "production_order_id", nullable = false, length = 64)
    private String productionOrderId;

    @Column(name = "customer_order_id", length = 64)
    private String customerOrderId;

    @Column(name = "client_id", nullable = false, length = 64)
    private String clientId;

    @Column(nullable = false, length = 16)
    private String concept;

    @Column(name = "source_delivery_id", length = 64)
    private String sourceDeliveryId;

    @Column(name = "source_addition_id", length = 64)
    private String sourceAdditionId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "payment_term_days", nullable = false)
    private int paymentTermDays;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public String getId() { return cxcDocumentId; }

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    @PostPersist
    void markNotNew() { this.isNew = false; }

    public void markNew() { this.isNew = true; }

    public String getCxcDocumentId() { return cxcDocumentId; }
    public void setCxcDocumentId(String cxcDocumentId) { this.cxcDocumentId = cxcDocumentId; }
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getCxcNumber() { return cxcNumber; }
    public void setCxcNumber(String cxcNumber) { this.cxcNumber = cxcNumber; }
    public String getProductionOrderId() { return productionOrderId; }
    public void setProductionOrderId(String productionOrderId) { this.productionOrderId = productionOrderId; }
    public String getCustomerOrderId() { return customerOrderId; }
    public void setCustomerOrderId(String customerOrderId) { this.customerOrderId = customerOrderId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getConcept() { return concept; }
    public void setConcept(String concept) { this.concept = concept; }
    public String getSourceDeliveryId() { return sourceDeliveryId; }
    public void setSourceDeliveryId(String sourceDeliveryId) { this.sourceDeliveryId = sourceDeliveryId; }
    public String getSourceAdditionId() { return sourceAdditionId; }
    public void setSourceAdditionId(String sourceAdditionId) { this.sourceAdditionId = sourceAdditionId; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public int getPaymentTermDays() { return paymentTermDays; }
    public void setPaymentTermDays(int paymentTermDays) { this.paymentTermDays = paymentTermDays; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
