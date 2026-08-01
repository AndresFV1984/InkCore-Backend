package com.inkcore.infrastructure.out.persistence.inkestimation.mapper;

import com.inkcore.domain.inkestimation.model.InkEstimateHistory;
import com.inkcore.infrastructure.out.persistence.inkestimation.entity.InkEstimateHistoryEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class InkEstimateHistoryPersistenceMapper {

    public InkEstimateHistoryEntity toNewEntity(InkEstimateHistory history) {
        InkEstimateHistoryEntity entity = new InkEstimateHistoryEntity();
        entity.setInkEstimateHistoryId(history.getInkEstimateHistoryId());
        copyScalars(history, entity);
        return entity;
    }

    public void copyScalars(InkEstimateHistory history, InkEstimateHistoryEntity entity) {
        entity.setOriginalFileName(history.getOriginalFileName());
        entity.setOriginalSizeBytes(history.getOriginalSizeBytes());
        entity.setMimeType(history.getMimeType());
        entity.setWidthCm(bd(history.getWidthCm(), 4));
        entity.setHeightCm(bd(history.getHeightCm(), 4));
        entity.setSheetCount(history.getSheetCount());
        entity.setDpi(history.getDpi());
        entity.setGramsPerCm2(bd(history.getGramsPerCm2(), 10));
        entity.setProcessGramsOrder(bd(history.getProcessGramsOrder(), 6));
        entity.setSpotGramsOrder(bd(history.getSpotGramsOrder(), 6));
        entity.setTotalGramsOrder(bd(history.getTotalGramsOrder(), 6));
        entity.setSpotCount(history.getSpotCount());
        entity.setIccProfileUsed(history.getIccProfileUsed());
        entity.setEstimateDate(history.getEstimateDate());
        entity.setDurationMs(history.getDurationMs());
        entity.setUserId(history.getUserId());
    }

    public InkEstimateHistory toDomain(InkEstimateHistoryEntity entity) {
        return InkEstimateHistory.reconstitute(
                entity.getInkEstimateHistoryId(),
                entity.getOriginalFileName(),
                entity.getOriginalSizeBytes(),
                entity.getMimeType(),
                entity.getWidthCm().doubleValue(),
                entity.getHeightCm().doubleValue(),
                entity.getSheetCount(),
                entity.getDpi(),
                entity.getGramsPerCm2().doubleValue(),
                entity.getProcessGramsOrder().doubleValue(),
                entity.getSpotGramsOrder().doubleValue(),
                entity.getTotalGramsOrder().doubleValue(),
                entity.getSpotCount(),
                entity.getIccProfileUsed(),
                entity.getEstimateDate(),
                entity.getDurationMs(),
                entity.getUserId()
        );
    }

    private static BigDecimal bd(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }
}
