package com.inkcore.application.assemblyprice.usecase;

import com.inkcore.domain.assemblyprice.model.AssemblyPrice;
import com.inkcore.domain.assemblyprice.ports.out.AssemblyPriceRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListAssemblyPricesUseCase {

    private final AssemblyPriceRepositoryPort assemblyPriceRepository;

    public ListAssemblyPricesUseCase(AssemblyPriceRepositoryPort assemblyPriceRepository) {
        this.assemblyPriceRepository = assemblyPriceRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<AssemblyPrice> execute(String companyId, Boolean state, PageQuery pageQuery) {
        PageQuery query = pageQuery == null ? PageQuery.of(0, PageQuery.DEFAULT_SIZE) : pageQuery;
        boolean filterCompany = companyId != null && !companyId.isBlank();
        if (filterCompany && state != null) {
            return assemblyPriceRepository.findPageByCompanyIdAndState(companyId.trim(), state, query);
        }
        if (filterCompany) {
            return assemblyPriceRepository.findPageByCompanyId(companyId.trim(), query);
        }
        if (state != null) {
            return assemblyPriceRepository.findPageByState(state, query);
        }
        return assemblyPriceRepository.findPage(query);
    }
}
