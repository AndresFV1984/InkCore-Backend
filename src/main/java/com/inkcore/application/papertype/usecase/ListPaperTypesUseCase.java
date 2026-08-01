package com.inkcore.application.papertype.usecase;

import com.inkcore.domain.papertype.model.PaperType;
import com.inkcore.domain.papertype.ports.out.PaperTypeRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListPaperTypesUseCase {

    private final PaperTypeRepositoryPort paperTypeRepository;

    public ListPaperTypesUseCase(PaperTypeRepositoryPort paperTypeRepository) {
        this.paperTypeRepository = paperTypeRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<PaperType> execute(String companyId, Boolean state, PageQuery pageQuery) {
        PageQuery query = pageQuery == null ? PageQuery.of(0, PageQuery.DEFAULT_SIZE) : pageQuery;
        boolean filterCompany = companyId != null && !companyId.isBlank();
        if (filterCompany && state != null) {
            return paperTypeRepository.findPageByCompanyIdAndState(companyId.trim(), state, query);
        }
        if (filterCompany) {
            return paperTypeRepository.findPageByCompanyId(companyId.trim(), query);
        }
        if (state != null) {
            return paperTypeRepository.findPageByState(state, query);
        }
        return paperTypeRepository.findPage(query);
    }
}
