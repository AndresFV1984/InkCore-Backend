package com.inkcore.domain.finishingprocess.ports.out;

import com.inkcore.domain.finishingprocess.model.FinishingProcess;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;

import java.util.Optional;

public interface FinishingProcessRepositoryPort {

    FinishingProcess save(FinishingProcess finishingProcess);

    Optional<FinishingProcess> findById(String finishingProcessId);

    PageResult<FinishingProcess> findPage(PageQuery pageQuery);

    PageResult<FinishingProcess> findPageByState(boolean state, PageQuery pageQuery);

    PageResult<FinishingProcess> findPageByCompanyId(String companyId, PageQuery pageQuery);

    PageResult<FinishingProcess> findPageByCompanyIdAndState(String companyId, boolean state, PageQuery pageQuery);

    boolean existsByCompanyIdAndNameIgnoreCase(String companyId, String name);

    boolean existsByCompanyIdAndNameIgnoreCaseExcludingFinishingProcessId(
            String companyId,
            String name,
            String finishingProcessId
    );
}
