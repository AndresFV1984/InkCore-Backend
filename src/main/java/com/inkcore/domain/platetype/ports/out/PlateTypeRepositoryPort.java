package com.inkcore.domain.platetype.ports.out;

import com.inkcore.domain.platetype.model.PlateType;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;

import java.util.Optional;

public interface PlateTypeRepositoryPort {

    PlateType save(PlateType plateType);

    Optional<PlateType> findById(String plateTypeId);

    PageResult<PlateType> findPage(PageQuery pageQuery);

    PageResult<PlateType> findPageByState(boolean state, PageQuery pageQuery);

    PageResult<PlateType> findPageByCompanyId(String companyId, PageQuery pageQuery);

    PageResult<PlateType> findPageByCompanyIdAndState(String companyId, boolean state, PageQuery pageQuery);

    boolean existsByCompanyIdAndNameIgnoreCase(String companyId, String name);

    boolean existsByCompanyIdAndNameIgnoreCaseExcludingPlateTypeId(
            String companyId,
            String name,
            String plateTypeId
    );
}
