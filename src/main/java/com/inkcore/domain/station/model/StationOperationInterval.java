package com.inkcore.domain.station.model;

import java.time.LocalDateTime;
import java.util.UUID;

public final class StationOperationInterval {

    private String intervalId;
    private String companyId;
    private String productionOrderId;
    private String clientId;
    private String userId;
    private String processKey;
    private String phase;
    private String catalogItemId;
    private StationIntervalKind intervalKind;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Long durationMs;
    private String pauseReason;
    private String note;
    private String openedByEventId;
    private String closedByEventId;
    private boolean open;
    private LocalDateTime createdAt;

    public StationOperationInterval() {
        this.intervalId = UUID.randomUUID().toString();
    }

    public String getIntervalId() {
        return intervalId;
    }

    public void setIntervalId(String intervalId) {
        this.intervalId = intervalId;
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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
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

    public StationIntervalKind getIntervalKind() {
        return intervalKind;
    }

    public void setIntervalKind(StationIntervalKind intervalKind) {
        this.intervalKind = intervalKind;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getPauseReason() {
        return pauseReason;
    }

    public void setPauseReason(String pauseReason) {
        this.pauseReason = pauseReason;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getOpenedByEventId() {
        return openedByEventId;
    }

    public void setOpenedByEventId(String openedByEventId) {
        this.openedByEventId = openedByEventId;
    }

    public String getClosedByEventId() {
        return closedByEventId;
    }

    public void setClosedByEventId(String closedByEventId) {
        this.closedByEventId = closedByEventId;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
