package com.inkcore.domain.cutlayout.ports.out;

import com.inkcore.domain.cutlayout.model.CutLayout;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;

import java.util.Optional;

public interface CutLayoutRepositoryPort {

    CutLayout save(CutLayout cutLayout);

    Optional<CutLayout> findById(String cutLayoutId);

    PageResult<CutLayout> findPage(PageQuery pageQuery);

    PageResult<CutLayout> findPageByState(boolean state, PageQuery pageQuery);

    PageResult<CutLayout> findPageByCompanyId(String companyId, PageQuery pageQuery);

    PageResult<CutLayout> findPageByCompanyIdAndState(String companyId, boolean state, PageQuery pageQuery);

    boolean existsByCompanyIdAndNameIgnoreCase(String companyId, String name);

    boolean existsByCompanyIdAndNameIgnoreCaseExcludingCutLayoutId(
            String companyId,
            String name,
            String cutLayoutId
    );
}
