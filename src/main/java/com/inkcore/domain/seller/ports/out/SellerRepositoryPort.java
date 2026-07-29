package com.inkcore.domain.seller.ports.out;

import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.seller.model.Seller;

import java.util.Optional;

public interface SellerRepositoryPort {

    Seller save(Seller seller);

    Optional<Seller> findById(String sellerId);

    PageResult<Seller> findPage(PageQuery pageQuery);

    PageResult<Seller> findPageByState(boolean state, PageQuery pageQuery);

    PageResult<Seller> findPageByCompanyId(String companyId, PageQuery pageQuery);

    PageResult<Seller> findPageByCompanyIdAndState(String companyId, boolean state, PageQuery pageQuery);

    boolean existsByCompanyIdAndIdentificationIgnoreCase(String companyId, String identification);

    boolean existsByCompanyIdAndIdentificationIgnoreCaseExcludingSellerId(
            String companyId,
            String identification,
            String sellerId
    );
}
