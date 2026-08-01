package com.inkcore.infrastructure.out.persistence.papertype.mapper;

import com.inkcore.domain.papertype.model.PaperType;
import com.inkcore.domain.papertype.model.PaperTypeCutAssignment;
import com.inkcore.infrastructure.out.persistence.papertype.entity.PaperTypeCutLayoutEntity;
import com.inkcore.infrastructure.out.persistence.papertype.entity.PaperTypeCutLayoutEntity.PaperTypeCutLayoutId;
import com.inkcore.infrastructure.out.persistence.papertype.entity.PaperTypeEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PaperTypePersistenceMapper {

    public PaperTypeEntity toNewEntity(PaperType paperType) {
        PaperTypeEntity e = new PaperTypeEntity();
        copyScalars(paperType, e);
        return e;
    }

    public void copyScalars(PaperType paperType, PaperTypeEntity e) {
        e.setPaperTypeId(paperType.getPaperTypeId());
        e.setCompanyId(paperType.getCompanyId());
        e.setName(paperType.getName());
        e.setWidth(paperType.getWidth());
        e.setHeight(paperType.getHeight());
        e.setUnit(paperType.getUnit());
        e.setSheetValue(paperType.getSheetValue());
        e.setPackageUnit(paperType.getPackageUnit());
        e.setCoated(paperType.isCoated());
        e.setState(paperType.isState());
        e.setCreationDate(paperType.getCreationDate());
    }

    public PaperType toDomain(PaperTypeEntity entity, List<PaperTypeCutLayoutEntity> assignments) {
        List<PaperTypeCutAssignment> cutAssignments = assignments == null
                ? List.of()
                : assignments.stream()
                .map(a -> PaperTypeCutAssignment.of(a.getId().getCutLayoutId(), a.getCutValue()))
                .toList();
        return PaperType.reconstitute(
                entity.getPaperTypeId(),
                entity.getCompanyId(),
                entity.getName(),
                entity.getWidth(),
                entity.getHeight(),
                entity.getUnit(),
                entity.getSheetValue(),
                entity.getPackageUnit(),
                entity.isCoated(),
                entity.isState(),
                entity.getCreationDate(),
                cutAssignments
        );
    }

    public PaperTypeCutLayoutEntity toAssignmentEntity(
            String paperTypeId,
            PaperTypeCutAssignment assignment,
            LocalDateTime assignedAt
    ) {
        PaperTypeCutLayoutEntity entity = new PaperTypeCutLayoutEntity();
        entity.setId(new PaperTypeCutLayoutId(paperTypeId, assignment.getCutLayoutId()));
        entity.setCutValue(assignment.getCutValue());
        entity.setAssignedAt(assignedAt);
        return entity;
    }
}
