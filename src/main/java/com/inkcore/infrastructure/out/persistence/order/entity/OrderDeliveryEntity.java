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
@Table(name = "order_deliveries", schema = "indicolors")
public class OrderDeliveryEntity implements Persistable<String> {

    @Id
    @Column(name = "order_delivery_id", length = 64)
    private String orderDeliveryId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "production_order_id", nullable = false, length = 64)
    private String productionOrderId;

    @Column(name = "client_id", nullable = false, length = 64)
    private String clientId;

    @Column(name = "seller_id", length = 64)
    private String sellerId;

    @Column(name = "delivery_type", nullable = false, length = 16)
    private String deliveryType;

    @Column(name = "quantity_delivered", nullable = false)
    private int quantityDelivered;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "available_before", nullable = false)
    private int availableBefore;

    @Column(name = "work_name_snapshot", length = 150)
    private String workNameSnapshot;

    @Column(name = "client_name_snapshot", length = 200)
    private String clientNameSnapshot;

    @Column(name = "delivered_at", nullable = false)
    private LocalDateTime deliveredAt;

    @Column(name = "delivered_by", nullable = false, length = 64)
    private String deliveredBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Override
    public String getId() { return orderDeliveryId; }

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    @PostPersist
    void markNotNew() { this.isNew = false; }

    public void markNew() { this.isNew = true; }

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
    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }
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
