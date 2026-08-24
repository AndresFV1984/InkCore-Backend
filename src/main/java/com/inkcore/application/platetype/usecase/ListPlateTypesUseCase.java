package com.inkcore.application.platetype.usecase;

import com.inkcore.domain.platetype.model.PlateType;
import com.inkcore.domain.platetype.ports.out.PlateTypeRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListPlateTypesUseCase {

    private final PlateTypeRepositoryPort plateTypeRepository;

    public ListPlateTypesUseCase(PlateTypeRepositoryPort plateTypeRepository) {
        this.plateTypeRepository = plateTypeRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<PlateType> execute(String companyId, Boolean state, PageQuery pageQuery) {
        PageQuery query = pageQuery == null ? PageQuery.of(0, PageQuery.DEFAULT_SIZE) : pageQuery;
        boolean filterCompany = companyId != null && !companyId.isBlank();
        if (filterCompany && state != null) {
            return plateTypeRepository.findPageByCompanyIdAndState(companyId.trim(), state, query);
        }
        if (filterCompany) {
            return plateTypeRepository.findPageByCompanyId(companyId.trim(), query);
        }
        if (state != null) {
            return plateTypeRepository.findPageByState(state, query);
        }
        return plateTypeRepository.findPage(query);
    }
}
