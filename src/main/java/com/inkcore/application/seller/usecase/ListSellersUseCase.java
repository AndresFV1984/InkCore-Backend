package com.inkcore.application.seller.usecase;

import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.seller.model.Seller;
import com.inkcore.domain.seller.ports.out.SellerRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListSellersUseCase {

    private final SellerRepositoryPort sellerRepository;

    public ListSellersUseCase(SellerRepositoryPort sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    /**
     * @param companyId opcional; si se envía, filtra por empresa
     * @param state     {@code null} = todos; {@code true}/{@code false} = filtro por estado
     */
    @Transactional(readOnly = true)
    public PageResult<Seller> execute(String companyId, Boolean state, PageQuery pageQuery) {
        PageQuery query = pageQuery == null ? PageQuery.of(0, PageQuery.DEFAULT_SIZE) : pageQuery;
        boolean filterCompany = companyId != null && !companyId.isBlank();
        if (filterCompany && state != null) {
            return sellerRepository.findPageByCompanyIdAndState(companyId.trim(), state, query);
        }
        if (filterCompany) {
            return sellerRepository.findPageByCompanyId(companyId.trim(), query);
        }
        if (state != null) {
            return sellerRepository.findPageByState(state, query);
        }
        return sellerRepository.findPage(query);
    }
}
