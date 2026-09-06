package com.inkcore.domain.order.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class ArSummary {

    private String companyId;
    private String productionOrderId;
    private String clientId;
    private int totalUnits;
    private int deliveredUnits;
    private int pendingUnits;
    private BigDecimal totalOwed;
    private BigDecimal totalPaid;
    private BigDecimal totalRemaining;
    private ArStatus status;
    private LocalDateTime lastDeliveryAt;
    private LocalDateTime lastPaymentAt;
    private LocalDateTime updatedAt;

    public ArSummary() {
        this.totalOwed = BigDecimal.ZERO;
        this.totalPaid = BigDecimal.ZERO;
        this.totalRemaining = BigDecimal.ZERO;
        this.status = ArStatus.PENDIENTE;
    }

    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getProductionOrderId() { return productionOrderId; }
    public void setProductionOrderId(String productionOrderId) { this.productionOrderId = productionOrderId; }
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
    public ArStatus getStatus() { return status; }
    public void setStatus(ArStatus status) { this.status = status; }
    public LocalDateTime getLastDeliveryAt() { return lastDeliveryAt; }
    public void setLastDeliveryAt(LocalDateTime lastDeliveryAt) { this.lastDeliveryAt = lastDeliveryAt; }
    public LocalDateTime getLastPaymentAt() { return lastPaymentAt; }
    public void setLastPaymentAt(LocalDateTime lastPaymentAt) { this.lastPaymentAt = lastPaymentAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
