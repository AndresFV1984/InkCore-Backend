package com.inkcore.application.finish.usecase;

import com.inkcore.domain.finish.model.Finish;
import com.inkcore.domain.finish.ports.out.FinishRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListFinishesUseCase {

    private final FinishRepositoryPort finishRepository;

    public ListFinishesUseCase(FinishRepositoryPort finishRepository) {
        this.finishRepository = finishRepository;
    }

    /**
     * @param companyId opcional; si se envía, filtra por empresa
     * @param state     {@code null} = todos; {@code true}/{@code false} = filtro por estado
     */
    @Transactional(readOnly = true)
    public PageResult<Finish> execute(String companyId, Boolean state, PageQuery pageQuery) {
        PageQuery query = pageQuery == null ? PageQuery.of(0, PageQuery.DEFAULT_SIZE) : pageQuery;
        boolean filterCompany = companyId != null && !companyId.isBlank();
        if (filterCompany && state != null) {
            return finishRepository.findPageByCompanyIdAndState(companyId.trim(), state, query);
        }
        if (filterCompany) {
            return finishRepository.findPageByCompanyId(companyId.trim(), query);
        }
        if (state != null) {
            return finishRepository.findPageByState(state, query);
        }
        return finishRepository.findPage(query);
    }
}
