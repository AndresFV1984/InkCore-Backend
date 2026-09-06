package com.inkcore.infrastructure.out.persistence.station.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "station_process_progress", schema = "indicolors")
@IdClass(StationProcessProgressEntity.IdKey.class)
public class StationProcessProgressEntity implements Persistable<StationProcessProgressEntity.IdKey> {

    @Id
    @Column(name = "production_order_id", length = 64)
    private String productionOrderId;

    @Id
    @Column(name = "process_key", length = 128)
    private String processKey;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(nullable = false, length = 32)
    private String phase;

    @Column(name = "catalog_item_id", length = 64)
    private String catalogItemId;

    @Column(name = "total_units", nullable = false)
    private int totalUnits;

    @Column(name = "completed_units", nullable = false)
    private int completedUnits;

    @Column(name = "delivered_units", nullable = false)
    private int deliveredUnits;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "last_event_at")
    private LocalDateTime lastEventAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public record IdKey(String productionOrderId, String processKey) implements Serializable {
        public IdKey {
            Objects.requireNonNull(productionOrderId);
            Objects.requireNonNull(processKey);
        }
    }

    @Override
    public IdKey getId() {
        return new IdKey(productionOrderId, processKey);
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

    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(String catalogItemId) {
        this.catalogItemId = catalogItemId;
    }

    public int getTotalUnits() {
        return totalUnits;
    }

    public void setTotalUnits(int totalUnits) {
        this.totalUnits = totalUnits;
    }

    public int getCompletedUnits() {
        return completedUnits;
    }

    public void setCompletedUnits(int completedUnits) {
        this.completedUnits = completedUnits;
    }

    public int getDeliveredUnits() {
        return deliveredUnits;
    }

    public void setDeliveredUnits(int deliveredUnits) {
        this.deliveredUnits = deliveredUnits;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastEventAt() {
        return lastEventAt;
    }

    public void setLastEventAt(LocalDateTime lastEventAt) {
        this.lastEventAt = lastEventAt;
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
