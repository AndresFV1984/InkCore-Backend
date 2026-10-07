package com.inkcore.domain.paper.ports.out;

import com.inkcore.domain.paper.model.Paper;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;

import java.math.BigDecimal;
import java.util.Optional;

public interface PaperRepositoryPort {

    Paper save(Paper paper);

    Optional<Paper> findById(String paperId);

    PageResult<Paper> findPage(
            String companyId,
            Boolean state,
            Boolean coated,
            Boolean acceptsRemnants,
            PageQuery pageQuery
    );

    boolean existsDuplicate(
            String companyId,
            String name,
            BigDecimal grammage,
            BigDecimal width,
            BigDecimal height,
            String unit
    );

    boolean existsDuplicateExcludingId(
            String companyId,
            String name,
            BigDecimal grammage,
            BigDecimal width,
            BigDecimal height,
            String unit,
            String paperId
    );
}
