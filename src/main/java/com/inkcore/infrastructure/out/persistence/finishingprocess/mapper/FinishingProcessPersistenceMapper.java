package com.inkcore.infrastructure.out.persistence.finishingprocess.mapper;

import com.inkcore.domain.finishingprocess.model.FinishingProcess;
import com.inkcore.infrastructure.out.persistence.finishingprocess.entity.FinishingProcessEntity;
import org.springframework.stereotype.Component;

@Component
public class FinishingProcessPersistenceMapper {

    public FinishingProcessEntity toNewEntity(FinishingProcess process) {
        FinishingProcessEntity e = new FinishingProcessEntity();
        copyScalars(process, e);
        return e;
    }

    public void copyScalars(FinishingProcess process, FinishingProcessEntity e) {
        e.setFinishingProcessId(process.getFinishingProcessId());
        e.setCompanyId(process.getCompanyId());
        e.setName(process.getName());
        e.setMinCost(process.getMinCost());
        e.setValuePerCm2(process.getValuePerCm2());
        e.setQuickAccess(process.isQuickAccess());
        e.setState(process.isState());
        e.setCreationDate(process.getCreationDate());
    }

    public FinishingProcess toDomain(FinishingProcessEntity entity) {
        return FinishingProcess.reconstitute(
                entity.getFinishingProcessId(),
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
