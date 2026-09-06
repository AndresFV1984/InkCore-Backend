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
@Table(name = "station_operation_intervals", schema = "indicolors")
public class StationOperationIntervalEntity implements Persistable<String> {

    @Id
    @Column(name = "station_operation_interval_id", length = 64)
    private String intervalId;

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

    @Column(name = "process_key", length = 128)
    private String processKey;

    @Column(length = 32)
    private String phase;

    @Column(name = "catalog_item_id", length = 64)
    private String catalogItemId;

    @Column(name = "interval_kind", nullable = false, length = 16)
    private String intervalKind;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "pause_reason", length = 64)
    private String pauseReason;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "opened_by_event_id", nullable = false, length = 64)
    private String openedByEventId;

    @Column(name = "closed_by_event_id", length = 64)
    private String closedByEventId;

    @Column(name = "is_open", nullable = false)
    private boolean open;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Override
    public String getId() {
        return intervalId;
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

    public String getIntervalKind() {
        return intervalKind;
    }

    public void setIntervalKind(String intervalKind) {
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
