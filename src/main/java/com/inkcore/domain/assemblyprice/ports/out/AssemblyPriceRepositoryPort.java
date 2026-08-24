package com.inkcore.domain.assemblyprice.ports.out;

import com.inkcore.domain.assemblyprice.model.AssemblyPrice;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;

import java.util.Optional;

public interface AssemblyPriceRepositoryPort {

    AssemblyPrice save(AssemblyPrice assemblyPrice);

    Optional<AssemblyPrice> findById(String assemblyPriceId);

    PageResult<AssemblyPrice> findPage(PageQuery pageQuery);

    PageResult<AssemblyPrice> findPageByState(boolean state, PageQuery pageQuery);

    PageResult<AssemblyPrice> findPageByCompanyId(String companyId, PageQuery pageQuery);

    PageResult<AssemblyPrice> findPageByCompanyIdAndState(String companyId, boolean state, PageQuery pageQuery);

    boolean existsByCompanyIdAndNameIgnoreCase(String companyId, String name);

    boolean existsByCompanyIdAndNameIgnoreCaseExcludingAssemblyPriceId(
            String companyId,
            String name,
            String assemblyPriceId
    );
}
