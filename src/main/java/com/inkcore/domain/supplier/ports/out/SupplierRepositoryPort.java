package com.inkcore.domain.supplier.ports.out;

import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.supplier.model.Supplier;

import java.util.Optional;

public interface SupplierRepositoryPort {

    Supplier save(Supplier supplier);

    Optional<Supplier> findById(String supplierId);

    PageResult<Supplier> findPage(PageQuery pageQuery);

    PageResult<Supplier> findPageByState(boolean state, PageQuery pageQuery);

    PageResult<Supplier> findPageByCompanyId(String companyId, PageQuery pageQuery);

    PageResult<Supplier> findPageByCompanyIdAndState(String companyId, boolean state, PageQuery pageQuery);

    boolean existsByCompanyIdAndIdentificationIgnoreCase(String companyId, String identification);

    boolean existsByCompanyIdAndIdentificationIgnoreCaseExcludingSupplierId(
            String companyId,
            String identification,
            String supplierId
    );
}
