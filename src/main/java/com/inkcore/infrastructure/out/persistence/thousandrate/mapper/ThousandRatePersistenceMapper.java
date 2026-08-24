package com.inkcore.infrastructure.out.persistence.thousandrate.mapper;

import com.inkcore.domain.thousandrate.model.ThousandRate;
import com.inkcore.infrastructure.out.persistence.thousandrate.entity.ThousandRateEntity;
import org.springframework.stereotype.Component;

@Component
public class ThousandRatePersistenceMapper {

    public ThousandRateEntity toNewEntity(ThousandRate thousandRate) {
        ThousandRateEntity e = new ThousandRateEntity();
        copyScalars(thousandRate, e);
        return e;
    }

    public void copyScalars(ThousandRate thousandRate, ThousandRateEntity e) {
        e.setThousandRateId(thousandRate.getThousandRateId());
        e.setCompanyId(thousandRate.getCompanyId());
        e.setName(thousandRate.getName());
        e.setColorCategory(thousandRate.getColorCategory());
        e.setThousandUnit(thousandRate.getThousandUnit());
        e.setPrice(thousandRate.getPrice());
        e.setState(thousandRate.isState());
        e.setMinThresholdUnits(thousandRate.getMinThresholdUnits());
        e.setMinThousand(thousandRate.getMinThousand());
        e.setDecimalThreshold(thousandRate.getDecimalThreshold());
        e.setGripperFlipPrice(thousandRate.getGripperFlipPrice());
        e.setSquareFlipPrice(thousandRate.getSquareFlipPrice());
        e.setDefault(thousandRate.isDefault());
        e.setCreationDate(thousandRate.getCreationDate());
    }

    public ThousandRate toDomain(ThousandRateEntity entity) {
        return ThousandRate.reconstitute(
                entity.getThousandRateId(),
                entity.getCompanyId(),
                entity.getName(),
                entity.getColorCategory(),
                entity.getThousandUnit(),
                entity.getPrice(),
                entity.isState(),
                entity.getMinThresholdUnits(),
                entity.getMinThousand(),
                entity.getDecimalThreshold(),
                entity.getGripperFlipPrice(),
                entity.getSquareFlipPrice(),
                entity.isDefault(),
                entity.getCreationDate()
        );
    }
}
