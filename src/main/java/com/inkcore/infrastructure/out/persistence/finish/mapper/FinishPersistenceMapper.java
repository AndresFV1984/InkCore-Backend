package com.inkcore.infrastructure.out.persistence.finish.mapper;

import com.inkcore.domain.finish.model.Finish;
import com.inkcore.infrastructure.out.persistence.finish.entity.FinishEntity;
import org.springframework.stereotype.Component;

@Component
public class FinishPersistenceMapper {

    public FinishEntity toNewEntity(Finish finish) {
        FinishEntity e = new FinishEntity();
        copyScalars(finish, e);
        return e;
    }

    public void copyScalars(Finish finish, FinishEntity e) {
        e.setFinishId(finish.getFinishId());
        e.setCompanyId(finish.getCompanyId());
        e.setName(finish.getName());
        e.setMinCost(finish.getMinCost());
        e.setValuePerCm2(finish.getValuePerCm2());
        e.setQuickAccess(finish.isQuickAccess());
        e.setState(finish.isState());
        e.setCreationDate(finish.getCreationDate());
    }

    public Finish toDomain(FinishEntity entity) {
        return Finish.reconstitute(
                entity.getFinishId(),
                entity.getCompanyId(),
                entity.getName(),
                entity.getMinCost(),
                entity.getValuePerCm2(),
                entity.isQuickAccess(),
                entity.isState(),
                entity.getCreationDate()
        );
    }
}
