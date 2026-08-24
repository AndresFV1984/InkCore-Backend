package com.inkcore.application.thousandrate.usecase;

import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.thousandrate.model.ThousandRate;
import com.inkcore.domain.thousandrate.ports.out.ThousandRateRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListThousandRatesUseCase {

    private final ThousandRateRepositoryPort thousandRateRepository;

    public ListThousandRatesUseCase(ThousandRateRepositoryPort thousandRateRepository) {
        this.thousandRateRepository = thousandRateRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<ThousandRate> execute(String companyId, Boolean state, PageQuery pageQuery) {
        PageQuery query = pageQuery == null ? PageQuery.of(0, PageQuery.DEFAULT_SIZE) : pageQuery;
        boolean filterCompany = companyId != null && !companyId.isBlank();
        if (filterCompany && state != null) {
            return thousandRateRepository.findPageByCompanyIdAndState(companyId.trim(), state, query);
        }
        if (filterCompany) {
            return thousandRateRepository.findPageByCompanyId(companyId.trim(), query);
        }
        if (state != null) {
            return thousandRateRepository.findPageByState(state, query);
        }
        return thousandRateRepository.findPage(query);
    }
}
