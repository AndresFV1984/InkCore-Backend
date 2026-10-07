package com.inkcore.application.paper.usecase;

import com.inkcore.domain.paper.model.PaperSupplierPrice;
import com.inkcore.domain.paper.ports.out.PaperSupplierPriceRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListPaperPricesUseCase {

    private final PaperSupplierPriceRepositoryPort priceRepository;
    private final PaperAccess access;
    private final PaperSupport support;

    public ListPaperPricesUseCase(
            PaperSupplierPriceRepositoryPort priceRepository,
            PaperAccess access,
            PaperSupport support
    ) {
        this.priceRepository = priceRepository;
        this.access = access;
        this.support = support;
    }

    @Transactional(readOnly = true)
    public List<PaperSupplierPrice> execute(String paperId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        access.requirePaper(paperId, companyId);
        return priceRepository.findByPaperId(companyId, paperId);
    }
}
