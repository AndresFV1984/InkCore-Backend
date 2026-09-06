package com.inkcore.infrastructure.out.persistence.station.mapper;

import com.inkcore.domain.station.model.StationCatalogItemKind;
import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.domain.station.model.StationIntervalKind;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.model.StationOperationInterval;
import com.inkcore.domain.station.model.StationOrderProgress;
import com.inkcore.domain.station.model.StationProcessProgress;
import com.inkcore.domain.station.model.StationProcessStatus;
import com.inkcore.infrastructure.out.persistence.station.entity.StationOperationEventEntity;
import com.inkcore.infrastructure.out.persistence.station.entity.StationOperationIntervalEntity;
import com.inkcore.infrastructure.out.persistence.station.entity.StationOrderProgressEntity;
import com.inkcore.infrastructure.out.persistence.station.entity.StationProcessProgressEntity;

public final class StationPersistenceMapper {

    private StationPersistenceMapper() {
    }

    public static StationOperationEventEntity toEntity(StationOperationEvent event) {
        StationOperationEventEntity entity = new StationOperationEventEntity();
        entity.setEventId(event.getEventId());
        entity.setCompanyId(event.getCompanyId());
        entity.setProductionOrderId(event.getProductionOrderId());
        entity.setClientId(event.getClientId());
        entity.setUserId(event.getUserId());
        entity.setActorUserId(event.getActorUserId());
        entity.setActorName(event.getActorName());
        entity.setWorkName(event.getWorkName());
        entity.setPhase(event.getPhase());
        entity.setProcessKey(event.getProcessKey());
        entity.setCatalogItemKind(event.getCatalogItemKind() == null
                ? null
                : event.getCatalogItemKind().getDbValue());
        entity.setCatalogItemId(event.getCatalogItemId());
        entity.setCatalogItemLabel(event.getCatalogItemLabel());
        entity.setEventType(event.getEventType().getDbValue());
        entity.setOccurredAt(event.getOccurredAt());
        entity.setUnits(event.getUnits());
        entity.setPauseReason(event.getPauseReason());
        entity.setNote(event.getNote());
        entity.setProductionStatusSnapshot(event.getProductionStatusSnapshot());
        entity.setOrderStatusSnapshot(event.getOrderStatusSnapshot());
        entity.setShiftEvent(event.isShiftEvent());
        entity.setCreatedAt(event.getCreatedAt());
        return entity;
    }

    public static StationOperationEvent toDomain(StationOperationEventEntity entity) {
        StationOperationEvent event = new StationOperationEvent();
        event.setEventId(entity.getEventId());
        event.setCompanyId(entity.getCompanyId());
        event.setProductionOrderId(entity.getProductionOrderId());
        event.setClientId(entity.getClientId());
        event.setUserId(entity.getUserId());
        event.setActorUserId(entity.getActorUserId());
        event.setActorName(entity.getActorName());
        event.setWorkName(entity.getWorkName());
        event.setPhase(entity.getPhase());
        event.setProcessKey(entity.getProcessKey());
        event.setCatalogItemKind(StationCatalogItemKind.fromValue(entity.getCatalogItemKind()));
        event.setCatalogItemId(entity.getCatalogItemId());
        event.setCatalogItemLabel(entity.getCatalogItemLabel());
        event.setEventType(StationEventType.fromValue(entity.getEventType()));
        event.setOccurredAt(entity.getOccurredAt());
        event.setUnits(entity.getUnits());
        event.setPauseReason(entity.getPauseReason());
        event.setNote(entity.getNote());
        event.setProductionStatusSnapshot(entity.getProductionStatusSnapshot());
        event.setOrderStatusSnapshot(entity.getOrderStatusSnapshot());
        event.setShiftEvent(entity.isShiftEvent());
        event.setCreatedAt(entity.getCreatedAt());
        return event;
    }

    public static StationOperationIntervalEntity toEntity(StationOperationInterval interval) {
        StationOperationIntervalEntity entity = new StationOperationIntervalEntity();
        copyInterval(interval, entity);
        return entity;
    }

    public static void copyInterval(StationOperationInterval interval, StationOperationIntervalEntity entity) {
        entity.setIntervalId(interval.getIntervalId());
        entity.setCompanyId(interval.getCompanyId());
        entity.setProductionOrderId(interval.getProductionOrderId());
        entity.setClientId(interval.getClientId());
        entity.setUserId(interval.getUserId());
        entity.setProcessKey(interval.getProcessKey());
        entity.setPhase(interval.getPhase());
        entity.setCatalogItemId(interval.getCatalogItemId());
        entity.setIntervalKind(interval.getIntervalKind().getDbValue());
        entity.setStartedAt(interval.getStartedAt());
        entity.setEndedAt(interval.getEndedAt());
        entity.setDurationMs(interval.getDurationMs());
        entity.setPauseReason(interval.getPauseReason());
        entity.setNote(interval.getNote());
        entity.setOpenedByEventId(interval.getOpenedByEventId());
        entity.setClosedByEventId(interval.getClosedByEventId());
        entity.setOpen(interval.isOpen());
        entity.setCreatedAt(interval.getCreatedAt());
    }

    public static StationOperationInterval toDomain(StationOperationIntervalEntity entity) {
        StationOperationInterval interval = new StationOperationInterval();
        interval.setIntervalId(entity.getIntervalId());
        interval.setCompanyId(entity.getCompanyId());
        interval.setProductionOrderId(entity.getProductionOrderId());
        interval.setClientId(entity.getClientId());
        interval.setUserId(entity.getUserId());
        interval.setProcessKey(entity.getProcessKey());
        interval.setPhase(entity.getPhase());
        interval.setCatalogItemId(entity.getCatalogItemId());
        interval.setIntervalKind(StationIntervalKind.fromValue(entity.getIntervalKind()));
        interval.setStartedAt(entity.getStartedAt());
        interval.setEndedAt(entity.getEndedAt());
        interval.setDurationMs(entity.getDurationMs());
        interval.setPauseReason(entity.getPauseReason());
        interval.setNote(entity.getNote());
        interval.setOpenedByEventId(entity.getOpenedByEventId());
        interval.setClosedByEventId(entity.getClosedByEventId());
        interval.setOpen(entity.isOpen());
        interval.setCreatedAt(entity.getCreatedAt());
        return interval;
    }

    public static StationProcessProgressEntity toEntity(StationProcessProgress progress) {
        StationProcessProgressEntity entity = new StationProcessProgressEntity();
        copyProgress(progress, entity);
        return entity;
    }

    public static void copyProgress(StationProcessProgress progress, StationProcessProgressEntity entity) {
        entity.setProductionOrderId(progress.getProductionOrderId());
        entity.setProcessKey(progress.getProcessKey());
        entity.setCompanyId(progress.getCompanyId());
        entity.setUserId(progress.getUserId());
        entity.setPhase(progress.getPhase());
        entity.setCatalogItemId(progress.getCatalogItemId());
        entity.setTotalUnits(progress.getTotalUnits());
        entity.setCompletedUnits(progress.getCompletedUnits());
        entity.setDeliveredUnits(progress.getDeliveredUnits());
        entity.setStatus(progress.getStatus().getDbValue());
        entity.setLastEventAt(progress.getLastEventAt());
        entity.setCreatedAt(progress.getCreatedAt());
        entity.setUpdatedAt(progress.getUpdatedAt());
    }

    public static StationProcessProgress toDomain(StationProcessProgressEntity entity) {
        StationProcessProgress progress = new StationProcessProgress();
        progress.setProductionOrderId(entity.getProductionOrderId());
        progress.setProcessKey(entity.getProcessKey());
        progress.setCompanyId(entity.getCompanyId());
        progress.setUserId(entity.getUserId());
        progress.setPhase(entity.getPhase());
        progress.setCatalogItemId(entity.getCatalogItemId());
        progress.setTotalUnits(entity.getTotalUnits());
        progress.setCompletedUnits(entity.getCompletedUnits());
        progress.setDeliveredUnits(entity.getDeliveredUnits());
        progress.setStatus(StationProcessStatus.fromValue(entity.getStatus()));
        progress.setLastEventAt(entity.getLastEventAt());
        progress.setCreatedAt(entity.getCreatedAt());
        progress.setUpdatedAt(entity.getUpdatedAt());
        return progress;
    }

    public static StationOrderProgressEntity toEntity(StationOrderProgress progress) {
        StationOrderProgressEntity entity = new StationOrderProgressEntity();
        copyOrderProgress(progress, entity);
        entity.markNew();
        return entity;
    }

    public static void copyOrderProgress(StationOrderProgress progress, StationOrderProgressEntity entity) {
        entity.setProductionOrderId(progress.getProductionOrderId());
        entity.setCompanyId(progress.getCompanyId());
        entity.setCantidadDisponible(progress.getCantidadDisponible());
        entity.setUpdatedAt(progress.getUpdatedAt());
    }

    public static StationOrderProgress toDomain(StationOrderProgressEntity entity) {
        StationOrderProgress progress = new StationOrderProgress();
        progress.setProductionOrderId(entity.getProductionOrderId());
        progress.setCompanyId(entity.getCompanyId());
        progress.setCantidadDisponible(entity.getCantidadDisponible());
        progress.setUpdatedAt(entity.getUpdatedAt());
        return progress;
    }
}
