package com.inkcore.domain.papertype.ports.out;

import com.inkcore.domain.papertype.model.PaperType;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;

import java.util.Optional;

public interface PaperTypeRepositoryPort {

    PaperType save(PaperType paperType);

    Optional<PaperType> findById(String paperTypeId);

    PageResult<PaperType> findPage(PageQuery pageQuery);

    PageResult<PaperType> findPageByState(boolean state, PageQuery pageQuery);

    PageResult<PaperType> findPageByCompanyId(String companyId, PageQuery pageQuery);

    PageResult<PaperType> findPageByCompanyIdAndState(String companyId, boolean state, PageQuery pageQuery);

    boolean existsByCompanyIdAndNameIgnoreCase(String companyId, String name);

    boolean existsByCompanyIdAndNameIgnoreCaseExcludingPaperTypeId(
            String companyId,
            String name,
            String paperTypeId
    );
}
