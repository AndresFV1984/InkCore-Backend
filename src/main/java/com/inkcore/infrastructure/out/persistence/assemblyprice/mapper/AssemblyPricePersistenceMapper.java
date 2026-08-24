package com.inkcore.infrastructure.out.persistence.assemblyprice.mapper;

import com.inkcore.domain.assemblyprice.model.AssemblyPrice;
import com.inkcore.infrastructure.out.persistence.assemblyprice.entity.AssemblyPriceEntity;
import org.springframework.stereotype.Component;

@Component
public class AssemblyPricePersistenceMapper {

    public AssemblyPriceEntity toNewEntity(AssemblyPrice assemblyPrice) {
        AssemblyPriceEntity e = new AssemblyPriceEntity();
        copyScalars(assemblyPrice, e);
        return e;
    }

    public void copyScalars(AssemblyPrice assemblyPrice, AssemblyPriceEntity e) {
        e.setAssemblyPriceId(assemblyPrice.getAssemblyPriceId());
        e.setCompanyId(assemblyPrice.getCompanyId());
        e.setName(assemblyPrice.getName());
        e.setCost(assemblyPrice.getCost());
        e.setState(assemblyPrice.isState());
        e.setCreationDate(assemblyPrice.getCreationDate());
    }

    public AssemblyPrice toDomain(AssemblyPriceEntity entity) {
        return AssemblyPrice.reconstitute(
                entity.getAssemblyPriceId(),
                entity.getCompanyId(),
                entity.getName(),
                entity.getCost(),
                entity.isState(),
                entity.getCreationDate()
        );
    }
}
