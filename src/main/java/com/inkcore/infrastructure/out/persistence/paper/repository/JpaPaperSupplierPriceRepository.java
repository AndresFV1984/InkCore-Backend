package com.inkcore.infrastructure.out.persistence.paper.repository;

import com.inkcore.infrastructure.out.persistence.paper.entity.PaperSupplierPriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface JpaPaperSupplierPriceRepository extends JpaRepository<PaperSupplierPriceEntity, String> {

    List<PaperSupplierPriceEntity> findAllByCompanyIdAndPaperIdOrderBySupplierIdAsc(
            String companyId, String paperId);

    void deleteAllByCompanyIdAndPaperIdAndSupplierIdIn(
            String companyId, String paperId, Collection<String> supplierIds);
}
