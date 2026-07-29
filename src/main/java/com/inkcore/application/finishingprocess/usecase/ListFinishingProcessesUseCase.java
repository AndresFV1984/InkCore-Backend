package com.inkcore.application.finishingprocess.usecase;

import com.inkcore.domain.finishingprocess.model.FinishingProcess;
import com.inkcore.domain.finishingprocess.ports.out.FinishingProcessRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListFinishingProcessesUseCase {

    private final FinishingProcessRepositoryPort finishingProcessRepository;

    public ListFinishingProcessesUseCase(FinishingProcessRepositoryPort finishingProcessRepository) {
        this.finishingProcessRepository = finishingProcessRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<FinishingProcess> execute(String companyId, Boolean state, PageQuery pageQuery) {
        PageQuery query = pageQuery == null ? PageQuery.of(0, PageQuery.DEFAULT_SIZE) : pageQuery;
        boolean filterCompany = companyId != null && !companyId.isBlank();
        if (filterCompany && state != null) {
            return finishingProcessRepository.findPageByCompanyIdAndState(companyId.trim(), state, query);
        }
        if (filterCompany) {
            return finishingProcessRepository.findPageByCompanyId(companyId.trim(), query);
        }
        if (state != null) {
            return finishingProcessRepository.findPageByState(state, query);
        }
        return finishingProcessRepository.findPage(query);
    }
}
