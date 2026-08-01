package com.inkcore.infrastructure.out.persistence.cutlayout.mapper;

import com.inkcore.domain.cutlayout.model.CutLayout;
import com.inkcore.infrastructure.out.persistence.cutlayout.entity.CutLayoutEntity;
import org.springframework.stereotype.Component;

@Component
public class CutLayoutPersistenceMapper {

    public CutLayoutEntity toNewEntity(CutLayout cutLayout) {
        CutLayoutEntity e = new CutLayoutEntity();
        copyScalars(cutLayout, e);
        return e;
    }

    public void copyScalars(CutLayout cutLayout, CutLayoutEntity e) {
        e.setCutLayoutId(cutLayout.getCutLayoutId());
        e.setCompanyId(cutLayout.getCompanyId());
        e.setName(cutLayout.getName());
        e.setWidth(cutLayout.getWidth());
        e.setHeight(cutLayout.getHeight());
        e.setUnit(cutLayout.getUnit());
        e.setPiecesPerSheet(cutLayout.getPiecesPerSheet());
        e.setState(cutLayout.isState());
        e.setCreationDate(cutLayout.getCreationDate());
    }

    public CutLayout toDomain(CutLayoutEntity entity) {
        return CutLayout.reconstitute(
                entity.getCutLayoutId(),
                entity.getCompanyId(),
                entity.getName(),
                entity.getWidth(),
                entity.getHeight(),
                entity.getUnit(),
                entity.getPiecesPerSheet(),
                entity.isState(),
                entity.getCreationDate()
        );
    }
}
