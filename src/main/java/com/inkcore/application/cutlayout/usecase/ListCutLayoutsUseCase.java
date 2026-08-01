package com.inkcore.application.cutlayout.usecase;

import com.inkcore.domain.cutlayout.model.CutLayout;
import com.inkcore.domain.cutlayout.ports.out.CutLayoutRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListCutLayoutsUseCase {

    private final CutLayoutRepositoryPort cutLayoutRepository;

    public ListCutLayoutsUseCase(CutLayoutRepositoryPort cutLayoutRepository) {
        this.cutLayoutRepository = cutLayoutRepository;
    }

    /**
     * @param companyId opcional; si se envía, filtra por empresa
     * @param state     {@code null} = todos; {@code true}/{@code false} = filtro por estado
     */
    @Transactional(readOnly = true)
    public PageResult<CutLayout> execute(String companyId, Boolean state, PageQuery pageQuery) {
        PageQuery query = pageQuery == null ? PageQuery.of(0, PageQuery.DEFAULT_SIZE) : pageQuery;
        boolean filterCompany = companyId != null && !companyId.isBlank();
        if (filterCompany && state != null) {
            return cutLayoutRepository.findPageByCompanyIdAndState(companyId.trim(), state, query);
        }
        if (filterCompany) {
            return cutLayoutRepository.findPageByCompanyId(companyId.trim(), query);
        }
        if (state != null) {
            return cutLayoutRepository.findPageByState(state, query);
        }
        return cutLayoutRepository.findPage(query);
    }
}
