package com.inkcore.domain.station.model;

import java.time.LocalDateTime;

public final class StationProcessProgress {

    private String companyId;
    private String productionOrderId;
    private String processKey;
    private String userId;
    private String phase;
    private String catalogItemId;
    private int totalUnits;
    private int completedUnits;
    private int deliveredUnits;
    private StationProcessStatus status;
    private LocalDateTime lastEventAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
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

    public StationProcessStatus getStatus() {
        return status;
    }

    public void setStatus(StationProcessStatus status) {
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
