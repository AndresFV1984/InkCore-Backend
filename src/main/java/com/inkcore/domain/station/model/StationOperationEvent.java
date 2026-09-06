package com.inkcore.domain.station.model;

import java.time.LocalDateTime;
import java.util.UUID;

public final class StationOperationEvent {

    private String eventId;
    private String companyId;
    private String productionOrderId;
    private String clientId;
    private String userId;
    private String actorUserId;
    private String actorName;
    private String workName;
    private String phase;
    private String processKey;
    private StationCatalogItemKind catalogItemKind;
    private String catalogItemId;
    private String catalogItemLabel;
    private StationEventType eventType;
    private LocalDateTime occurredAt;
    private Integer units;
    private String pauseReason;
    private String note;
    private String productionStatusSnapshot;
    private String orderStatusSnapshot;
    private boolean shiftEvent;
    private LocalDateTime createdAt;

    public StationOperationEvent() {
        this.eventId = UUID.randomUUID().toString();
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
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

    public String getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(String actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getWorkName() {
        return workName;
    }

    public void setWorkName(String workName) {
        this.workName = workName;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
    }

    public StationCatalogItemKind getCatalogItemKind() {
        return catalogItemKind;
    }

    public void setCatalogItemKind(StationCatalogItemKind catalogItemKind) {
        this.catalogItemKind = catalogItemKind;
    }

    public String getCatalogItemId() {
        return catalogItemId;
    }

    public void setCatalogItemId(String catalogItemId) {
        this.catalogItemId = catalogItemId;
    }

    public String getCatalogItemLabel() {
        return catalogItemLabel;
    }

    public void setCatalogItemLabel(String catalogItemLabel) {
        this.catalogItemLabel = catalogItemLabel;
    }

    public StationEventType getEventType() {
        return eventType;
    }

    public void setEventType(StationEventType eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Integer getUnits() {
        return units;
    }

    public void setUnits(Integer units) {
        this.units = units;
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

    public String getProductionStatusSnapshot() {
        return productionStatusSnapshot;
    }

    public void setProductionStatusSnapshot(String productionStatusSnapshot) {
        this.productionStatusSnapshot = productionStatusSnapshot;
    }

    public String getOrderStatusSnapshot() {
        return orderStatusSnapshot;
    }

    public void setOrderStatusSnapshot(String orderStatusSnapshot) {
        this.orderStatusSnapshot = orderStatusSnapshot;
    }

    public boolean isShiftEvent() {
        return shiftEvent;
    }

    public void setShiftEvent(boolean shiftEvent) {
        this.shiftEvent = shiftEvent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
