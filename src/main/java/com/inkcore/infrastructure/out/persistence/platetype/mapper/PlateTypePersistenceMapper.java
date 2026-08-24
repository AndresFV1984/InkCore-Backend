package com.inkcore.infrastructure.out.persistence.platetype.mapper;

import com.inkcore.domain.platetype.model.PlateType;
import com.inkcore.infrastructure.out.persistence.platetype.entity.PlateTypeEntity;
import org.springframework.stereotype.Component;

@Component
public class PlateTypePersistenceMapper {

    public PlateTypeEntity toNewEntity(PlateType plateType) {
        PlateTypeEntity e = new PlateTypeEntity();
        copyScalars(plateType, e);
        return e;
    }

    public void copyScalars(PlateType plateType, PlateTypeEntity e) {
        e.setPlateTypeId(plateType.getPlateTypeId());
        e.setCompanyId(plateType.getCompanyId());
        e.setName(plateType.getName());
        e.setWidth(plateType.getWidth());
        e.setHeight(plateType.getHeight());
        e.setUnit(plateType.getUnit());
        e.setValue(plateType.getValue());
        e.setState(plateType.isState());
        e.setCreationDate(plateType.getCreationDate());
    }

    public PlateType toDomain(PlateTypeEntity entity) {
        return PlateType.reconstitute(
                entity.getPlateTypeId(),
                entity.getCompanyId(),
                entity.getName(),
                entity.getWidth(),
                entity.getHeight(),
                entity.getUnit(),
                entity.getValue(),
                entity.isState(),
                entity.getCreationDate()
        );
    }
}
