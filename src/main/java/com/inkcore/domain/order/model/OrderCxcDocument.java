package com.inkcore.domain.order.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Documento de cartera CxC (en producto = factura). */
public final class OrderCxcDocument {

    private String cxcDocumentId;
    private String companyId;
    private String cxcNumber;
    private String productionOrderId;
    private String customerOrderId;
    private String clientId;
    private CxcDocumentConcept concept;
    private String sourceDeliveryId;
    private String sourceAdditionId;
    private BigDecimal total;
    private CxcDocumentStatus status;
    private LocalDateTime issuedAt;
    private LocalDate dueDate;
    private int paymentTermDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OrderCxcDocument() {
        this.cxcDocumentId = UUID.randomUUID().toString();
        this.concept = CxcDocumentConcept.ENTREGA;
        this.status = CxcDocumentStatus.PENDIENTE;
        this.total = BigDecimal.ZERO;
    }

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
    public CxcDocumentConcept getConcept() { return concept; }
    public void setConcept(CxcDocumentConcept concept) { this.concept = concept; }
    public String getSourceDeliveryId() { return sourceDeliveryId; }
    public void setSourceDeliveryId(String sourceDeliveryId) { this.sourceDeliveryId = sourceDeliveryId; }
    public String getSourceAdditionId() { return sourceAdditionId; }
    public void setSourceAdditionId(String sourceAdditionId) { this.sourceAdditionId = sourceAdditionId; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public CxcDocumentStatus getStatus() { return status; }
    public void setStatus(CxcDocumentStatus status) { this.status = status; }
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
