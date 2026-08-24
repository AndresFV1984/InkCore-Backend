package com.inkcore.infrastructure.out.persistence.productionorder.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 1:1 con {@code production_orders}: comparte la misma clave primaria.
 */
@Entity
@Table(name = "production_order_billing_details", schema = "indicolors")
public class ProductionOrderBillingDetailsEntity implements Persistable<String> {

    @Id
    @Column(name = "production_order_id", length = 64)
    private String productionOrderId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "billing_discount_type", length = 10)
    private String billingDiscountType;

    @Column(name = "billing_discount_value", precision = 12, scale = 2)
    private BigDecimal billingDiscountValue;

    @Column(name = "client_costing_mode", length = 10)
    private String clientCostingMode;

    @Column(name = "client_discount_type", length = 12)
    private String clientDiscountType;

    @Column(name = "client_discount_value", precision = 12, scale = 2)
    private BigDecimal clientDiscountValue;

    @Column(name = "client_profitability_type", length = 12)
    private String clientProfitabilityType;

    @Column(name = "client_profitability_value", precision = 12, scale = 2)
    private BigDecimal clientProfitabilityValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "client_volume_costing")
    private List<Map<String, Object>> clientVolumeCosting;

    @Column(name = "delivery_start_date")
    private LocalDate deliveryStartDate;

    @Column(name = "delivery_end_date")
    private LocalDate deliveryEndDate;

    @Column(name = "advance_percentage", precision = 5, scale = 2)
    private BigDecimal advancePercentage;

    @Column(name = "client_signature_name", length = 150)
    private String clientSignatureName;

    @Column(name = "bank_account_id", length = 64)
    private String bankAccountId;

    @Column(name = "billing_completed_at")
    private LocalDateTime billingCompletedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ProductionOrderBillingDetailsEntity() {
    }

    @Override
    public String getId() {
        return productionOrderId;
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

    public String getBillingDiscountType() {
        return billingDiscountType;
    }

    public void setBillingDiscountType(String billingDiscountType) {
        this.billingDiscountType = billingDiscountType;
    }

    public BigDecimal getBillingDiscountValue() {
        return billingDiscountValue;
    }

    public void setBillingDiscountValue(BigDecimal billingDiscountValue) {
        this.billingDiscountValue = billingDiscountValue;
    }

    public String getClientCostingMode() {
        return clientCostingMode;
    }

    public void setClientCostingMode(String clientCostingMode) {
        this.clientCostingMode = clientCostingMode;
    }

    public String getClientDiscountType() {
        return clientDiscountType;
    }

    public void setClientDiscountType(String clientDiscountType) {
        this.clientDiscountType = clientDiscountType;
    }

    public BigDecimal getClientDiscountValue() {
        return clientDiscountValue;
    }

    public void setClientDiscountValue(BigDecimal clientDiscountValue) {
        this.clientDiscountValue = clientDiscountValue;
    }

    public String getClientProfitabilityType() {
        return clientProfitabilityType;
    }

    public void setClientProfitabilityType(String clientProfitabilityType) {
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
        this.clientVolumeCosting = clientVolumeCosting;
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
