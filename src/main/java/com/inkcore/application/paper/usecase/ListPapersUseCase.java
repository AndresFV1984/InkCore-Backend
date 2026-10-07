package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.model.Paper;
import com.inkcore.domain.paper.ports.out.PaperRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListPapersUseCase {

    private final PaperRepositoryPort paperRepository;
    private final PaperSupport support;

    public ListPapersUseCase(PaperRepositoryPort paperRepository, PaperSupport support) {
        this.paperRepository = paperRepository;
        this.support = support;
    }

    @Transactional(readOnly = true)
    public PageResult<Paper> execute(
            Boolean state,
            Boolean coated,
            Boolean acceptsRemnants,
            PageQuery pageQuery,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        PageQuery query = pageQuery == null ? PageQuery.of(0, PageQuery.DEFAULT_SIZE) : pageQuery;
        return paperRepository.findPage(companyId, state, coated, acceptsRemnants, query);
    }
}
