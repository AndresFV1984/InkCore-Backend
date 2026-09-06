package com.inkcore.infrastructure.out.persistence.station.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@Entity
@Table(name = "station_operation_events", schema = "indicolors")
public class StationOperationEventEntity implements Persistable<String> {

    @Id
    @Column(name = "station_operation_event_id", length = 64)
    private String eventId;

    @Transient
    private boolean isNew = true;

    @Column(name = "company_id", nullable = false, length = 64)
    private String companyId;

    @Column(name = "production_order_id", length = 64)
    private String productionOrderId;

    @Column(name = "client_id", length = 64)
    private String clientId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "actor_user_id", nullable = false, length = 64)
    private String actorUserId;

    @Column(name = "actor_name", length = 255)
    private String actorName;

    @Column(name = "work_name", length = 150)
    private String workName;

    @Column(nullable = false, length = 32)
    private String phase;

    @Column(name = "process_key", nullable = false, length = 128)
    private String processKey;

    @Column(name = "catalog_item_kind", length = 16)
    private String catalogItemKind;

    @Column(name = "catalog_item_id", length = 64)
    private String catalogItemId;

    @Column(name = "catalog_item_label", length = 255)
    private String catalogItemLabel;

    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    private Integer units;

    @Column(name = "pause_reason", length = 64)
    private String pauseReason;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "production_status_snapshot", length = 64)
    private String productionStatusSnapshot;

    @Column(name = "order_status_snapshot", length = 64)
    private String orderStatusSnapshot;

    @Column(name = "is_shift_event", nullable = false)
    private boolean shiftEvent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Override
    public String getId() {
        return eventId;
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

    public String getCatalogItemKind() {
        return catalogItemKind;
    }

    public void setCatalogItemKind(String catalogItemKind) {
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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
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
