package com.inkcore.domain.thousandrate.ports.out;

import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.thousandrate.model.ThousandRate;

import java.util.Optional;

public interface ThousandRateRepositoryPort {

    ThousandRate save(ThousandRate thousandRate);

    Optional<ThousandRate> findById(String thousandRateId);

    PageResult<ThousandRate> findPage(PageQuery pageQuery);

    PageResult<ThousandRate> findPageByState(boolean state, PageQuery pageQuery);

    PageResult<ThousandRate> findPageByCompanyId(String companyId, PageQuery pageQuery);

    PageResult<ThousandRate> findPageByCompanyIdAndState(String companyId, boolean state, PageQuery pageQuery);

    boolean existsByCompanyIdAndNameIgnoreCase(String companyId, String name);

    boolean existsByCompanyIdAndNameIgnoreCaseExcludingThousandRateId(
            String companyId,
            String name,
            String thousandRateId
    );

    /**
     * Deja como máximo un default por empresa + categoría de color:
     * desmarca isDefault en el resto excepto {@code thousandRateId}.
     */
    void clearDefaultForCompanyAndColorCategoryExcept(
            String companyId,
            String colorCategory,
            String thousandRateId
    );
}
