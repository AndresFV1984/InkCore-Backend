package com.inkcore.infrastructure.out.persistence.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ar_summary", schema = "indicolors")
public class ArSummaryEntity {

    @Id
    @Column(name = "production_order_id", length = 64)
    private String productionOrderId;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "client_id", nullable = false, length = 64)
    private String clientId;

    @Column(name = "total_units", nullable = false)
    private int totalUnits;

    @Column(name = "delivered_units", nullable = false)
    private int deliveredUnits;

    @Column(name = "pending_units", nullable = false)
    private int pendingUnits;

    @Column(name = "total_owed", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalOwed;

    @Column(name = "total_paid", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPaid;

    @Column(name = "total_remaining", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalRemaining;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "last_delivery_at")
    private LocalDateTime lastDeliveryAt;

    @Column(name = "last_payment_at")
    private LocalDateTime lastPaymentAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String getProductionOrderId() { return productionOrderId; }
    public void setProductionOrderId(String productionOrderId) { this.productionOrderId = productionOrderId; }
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public int getTotalUnits() { return totalUnits; }
    public void setTotalUnits(int totalUnits) { this.totalUnits = totalUnits; }
    public int getDeliveredUnits() { return deliveredUnits; }
    public void setDeliveredUnits(int deliveredUnits) { this.deliveredUnits = deliveredUnits; }
    public int getPendingUnits() { return pendingUnits; }
    public void setPendingUnits(int pendingUnits) { this.pendingUnits = pendingUnits; }
    public BigDecimal getTotalOwed() { return totalOwed; }
    public void setTotalOwed(BigDecimal totalOwed) { this.totalOwed = totalOwed; }
    public BigDecimal getTotalPaid() { return totalPaid; }
    public void setTotalPaid(BigDecimal totalPaid) { this.totalPaid = totalPaid; }
    public BigDecimal getTotalRemaining() { return totalRemaining; }
    public void setTotalRemaining(BigDecimal totalRemaining) { this.totalRemaining = totalRemaining; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLastDeliveryAt() { return lastDeliveryAt; }
    public void setLastDeliveryAt(LocalDateTime lastDeliveryAt) { this.lastDeliveryAt = lastDeliveryAt; }
    public LocalDateTime getLastPaymentAt() { return lastPaymentAt; }
    public void setLastPaymentAt(LocalDateTime lastPaymentAt) { this.lastPaymentAt = lastPaymentAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
