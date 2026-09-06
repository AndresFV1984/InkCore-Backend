package com.inkcore.domain.order.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class OrderDelivery {

    private String orderDeliveryId;
    private String companyId;
    private String productionOrderId;
    private String clientId;
    private String sellerId;
    private DeliveryType deliveryType;
    private int quantityDelivered;
    private BigDecimal unitPrice;
    private BigDecimal totalValue;
    private int availableBefore;
    private String workNameSnapshot;
    private String clientNameSnapshot;
    private LocalDateTime deliveredAt;
    private String deliveredBy;
    private String notes;
    private LocalDateTime createdAt;

    public OrderDelivery() {
        this.orderDeliveryId = UUID.randomUUID().toString();
        this.availableBefore = 0;
        this.unitPrice = BigDecimal.ZERO;
        this.totalValue = BigDecimal.ZERO;
    }

    public String getOrderDeliveryId() { return orderDeliveryId; }
    public void setOrderDeliveryId(String orderDeliveryId) { this.orderDeliveryId = orderDeliveryId; }
    public String getCompanyId() { return companyId; }
    public void setCompanyId(String companyId) { this.companyId = companyId; }
    public String getProductionOrderId() { return productionOrderId; }
    public void setProductionOrderId(String productionOrderId) { this.productionOrderId = productionOrderId; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public DeliveryType getDeliveryType() { return deliveryType; }
    public void setDeliveryType(DeliveryType deliveryType) { this.deliveryType = deliveryType; }
    public int getQuantityDelivered() { return quantityDelivered; }
    public void setQuantityDelivered(int quantityDelivered) { this.quantityDelivered = quantityDelivered; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public int getAvailableBefore() { return availableBefore; }
    public void setAvailableBefore(int availableBefore) { this.availableBefore = availableBefore; }
    public String getWorkNameSnapshot() { return workNameSnapshot; }
    public void setWorkNameSnapshot(String workNameSnapshot) { this.workNameSnapshot = workNameSnapshot; }
    public String getClientNameSnapshot() { return clientNameSnapshot; }
    public void setClientNameSnapshot(String clientNameSnapshot) { this.clientNameSnapshot = clientNameSnapshot; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
    public String getDeliveredBy() { return deliveredBy; }
    public void setDeliveredBy(String deliveredBy) { this.deliveredBy = deliveredBy; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
