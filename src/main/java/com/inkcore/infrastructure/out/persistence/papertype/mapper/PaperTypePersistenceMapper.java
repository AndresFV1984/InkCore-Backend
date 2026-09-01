package com.inkcore.infrastructure.out.persistence.papertype.mapper;

import com.inkcore.domain.papertype.model.PaperType;
import com.inkcore.domain.papertype.model.PaperTypeCutAssignment;
import com.inkcore.domain.papertype.model.PaperTypeSupplierAssignment;
import com.inkcore.infrastructure.out.persistence.papertype.entity.PaperTypeCutLayoutEntity;
import com.inkcore.infrastructure.out.persistence.papertype.entity.PaperTypeCutLayoutEntity.PaperTypeCutLayoutId;
import com.inkcore.infrastructure.out.persistence.papertype.entity.PaperTypeEntity;
import com.inkcore.infrastructure.out.persistence.papertype.entity.PaperTypeSupplierEntity;
import com.inkcore.infrastructure.out.persistence.papertype.entity.PaperTypeSupplierEntity.PaperTypeSupplierId;
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
        e.setCoated(paperType.isCoated());
        e.setState(paperType.isState());
        e.setCreationDate(paperType.getCreationDate());
    }

    public PaperType toDomain(
            PaperTypeEntity entity,
            List<PaperTypeCutLayoutEntity> cutAssignments,
            List<PaperTypeSupplierEntity> supplierAssignments
    ) {
        List<PaperTypeCutAssignment> cuts = cutAssignments == null
                ? List.of()
                : cutAssignments.stream()
                .map(a -> PaperTypeCutAssignment.of(a.getId().getCutLayoutId(), a.getCutValue()))
                .toList();
        List<PaperTypeSupplierAssignment> suppliers = supplierAssignments == null
                ? List.of()
                : supplierAssignments.stream()
                .map(a -> PaperTypeSupplierAssignment.of(
                        a.getId().getSupplierId(),
                        a.getSheetValue(),
                        a.getPackageUnit()
                ))
                .toList();
        return PaperType.reconstitute(
                entity.getPaperTypeId(),
                entity.getCompanyId(),
                entity.getName(),
                entity.getWidth(),
                entity.getHeight(),
                entity.getUnit(),
                entity.isCoated(),
                entity.isState(),
                entity.getCreationDate(),
                cuts,
                suppliers
        );
    }

    public PaperTypeCutLayoutEntity toCutAssignmentEntity(
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

    public PaperTypeSupplierEntity toSupplierAssignmentEntity(
            String paperTypeId,
            PaperTypeSupplierAssignment assignment,
            LocalDateTime assignedAt
    ) {
        PaperTypeSupplierEntity entity = new PaperTypeSupplierEntity();
        entity.setId(new PaperTypeSupplierId(paperTypeId, assignment.getSupplierId()));
        entity.setSheetValue(assignment.getSheetValue());
        entity.setPackageUnit(assignment.getPackageUnit());
        entity.setAssignedAt(assignedAt);
        return entity;
    }
}
