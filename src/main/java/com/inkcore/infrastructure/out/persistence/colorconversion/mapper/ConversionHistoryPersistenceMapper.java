package com.inkcore.infrastructure.out.persistence.colorconversion.mapper;

import com.inkcore.domain.colorconversion.model.ConversionHistory;
import com.inkcore.domain.colorconversion.model.RenderingIntent;
import com.inkcore.infrastructure.out.persistence.colorconversion.entity.ConversionHistoryEntity;
import org.springframework.stereotype.Component;

@Component
public class ConversionHistoryPersistenceMapper {

    public ConversionHistoryEntity toNewEntity(ConversionHistory history) {
        ConversionHistoryEntity entity = new ConversionHistoryEntity();
        entity.setConversionHistoryId(history.getConversionHistoryId());
        copyScalars(history, entity);
        return entity;
    }

    public void copyScalars(ConversionHistory history, ConversionHistoryEntity entity) {
        entity.setOriginalFileName(history.getOriginalFileName());
        entity.setOriginalSizeBytes(history.getOriginalSizeBytes());
        entity.setFinalSizeBytes(history.getFinalSizeBytes());
        entity.setRenderingIntent(history.getRenderingIntent().name());
        entity.setIccProfileUsed(history.getIccProfileUsed());
        entity.setConversionDate(history.getConversionDate());
        entity.setDurationMs(history.getDurationMs());
        entity.setUserId(history.getUserId());
        entity.setMimeType(history.getMimeType());
    }

    public ConversionHistory toDomain(ConversionHistoryEntity entity) {
        return ConversionHistory.reconstitute(
                entity.getConversionHistoryId(),
                entity.getOriginalFileName(),
                entity.getOriginalSizeBytes(),
                entity.getFinalSizeBytes(),
                RenderingIntent.valueOf(entity.getRenderingIntent()),
                entity.getIccProfileUsed(),
                entity.getConversionDate(),
                entity.getDurationMs(),
                entity.getUserId(),
                entity.getMimeType()
        );
    }
}
