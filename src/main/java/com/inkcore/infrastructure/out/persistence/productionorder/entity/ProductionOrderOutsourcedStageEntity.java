package com.inkcore.infrastructure.out.persistence.productionorder.entity;

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
@Table(name = "production_order_outsourced_stages", schema = "indicolors")
public class ProductionOrderOutsourcedStageEntity implements Persistable<String> {

    @Id
    @Column(name = "production_order_outsourced_stage_id", length = 64)
    private String productionOrderOutsourcedStageId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "production_order_id", nullable = false, length = 64)
    private String productionOrderId;

    @Column(name = "stage", nullable = false, length = 30)
    private String stage;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "vendor_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal vendorCost;

    @Column(name = "markup_percent", nullable = false, precision = 8, scale = 2)
    private BigDecimal markupPercent;

    @Column(name = "sell_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal sellPrice;

    @Column(name = "supplier_id", length = 64)
    private String supplierId;

    @Column(name = "supplier_name", length = 200)
    private String supplierName;

    @Column(name = "quoted_at")
    private LocalDate quotedAt;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "cost_responsible_user_id", length = 64)
    private String costResponsibleUserId;

    @Column(name = "cost_responsible_user_name", length = 200)
    private String costResponsibleUserName;

    @Column(name = "quote_valid_until")
    private LocalDate quoteValidUntil;

    @Column(name = "sent_at")
    private LocalDate sentAt;

    @Column(name = "sent_description", length = 1000)
    private String sentDescription;

    @Column(name = "expected_return_at")
    private LocalDate expectedReturnAt;

    @Column(name = "received_at")
    private LocalDate receivedAt;

    @Column(name = "received_by_user_id", length = 64)
    private String receivedByUserId;

    @Column(name = "fulfillment_status", nullable = false, length = 20)
    private String fulfillmentStatus = "PENDING";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ProductionOrderOutsourcedStageEntity() {
    }

    @Override
    public String getId() {
        return productionOrderOutsourcedStageId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    public String getProductionOrderOutsourcedStageId() {
        return productionOrderOutsourcedStageId;
    }

    public void setProductionOrderOutsourcedStageId(String productionOrderOutsourcedStageId) {
        this.productionOrderOutsourcedStageId = productionOrderOutsourcedStageId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getProductionOrderId() {
        return productionOrderId;
    }

    public void setProductionOrderId(String productionOrderId) {
        this.productionOrderId = productionOrderId;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public BigDecimal getVendorCost() {
        return vendorCost;
    }

    public void setVendorCost(BigDecimal vendorCost) {
        this.vendorCost = vendorCost;
    }

    public BigDecimal getMarkupPercent() {
        return markupPercent;
    }

    public void setMarkupPercent(BigDecimal markupPercent) {
        this.markupPercent = markupPercent;
    }

    public BigDecimal getSellPrice() {
        return sellPrice;
    }

    public void setSellPrice(BigDecimal sellPrice) {
        this.sellPrice = sellPrice;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public LocalDate getQuotedAt() {
        return quotedAt;
    }

    public void setQuotedAt(LocalDate quotedAt) {
        this.quotedAt = quotedAt;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getCostResponsibleUserId() {
        return costResponsibleUserId;
    }

    public void setCostResponsibleUserId(String costResponsibleUserId) {
        this.costResponsibleUserId = costResponsibleUserId;
    }

    public String getCostResponsibleUserName() {
        return costResponsibleUserName;
    }

    public void setCostResponsibleUserName(String costResponsibleUserName) {
        this.costResponsibleUserName = costResponsibleUserName;
    }

    public LocalDate getQuoteValidUntil() {
        return quoteValidUntil;
    }

    public void setQuoteValidUntil(LocalDate quoteValidUntil) {
        this.quoteValidUntil = quoteValidUntil;
    }

    public LocalDate getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDate sentAt) {
        this.sentAt = sentAt;
    }

    public String getSentDescription() {
        return sentDescription;
    }

    public void setSentDescription(String sentDescription) {
        this.sentDescription = sentDescription;
    }

    public LocalDate getExpectedReturnAt() {
        return expectedReturnAt;
    }

    public void setExpectedReturnAt(LocalDate expectedReturnAt) {
        this.expectedReturnAt = expectedReturnAt;
    }

    public LocalDate getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDate receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getReceivedByUserId() {
        return receivedByUserId;
    }

    public void setReceivedByUserId(String receivedByUserId) {
        this.receivedByUserId = receivedByUserId;
    }

    public String getFulfillmentStatus() {
        return fulfillmentStatus;
    }

    public void setFulfillmentStatus(String fulfillmentStatus) {
        this.fulfillmentStatus = fulfillmentStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
