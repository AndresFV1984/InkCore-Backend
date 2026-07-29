package com.inkcore.domain.finish.ports.out;

import com.inkcore.domain.finish.model.Finish;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;

import java.util.Optional;

public interface FinishRepositoryPort {

    Finish save(Finish finish);

    Optional<Finish> findById(String finishId);

    PageResult<Finish> findPage(PageQuery pageQuery);

    PageResult<Finish> findPageByState(boolean state, PageQuery pageQuery);

    PageResult<Finish> findPageByCompanyId(String companyId, PageQuery pageQuery);

    PageResult<Finish> findPageByCompanyIdAndState(String companyId, boolean state, PageQuery pageQuery);

    boolean existsByCompanyIdAndNameIgnoreCase(String companyId, String name);

    boolean existsByCompanyIdAndNameIgnoreCaseExcludingFinishId(
            String companyId,
            String name,
            String finishId
    );
}
