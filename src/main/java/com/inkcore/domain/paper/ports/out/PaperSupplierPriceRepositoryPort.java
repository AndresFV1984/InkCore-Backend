package com.inkcore.domain.paper.ports.out;

import com.inkcore.domain.paper.model.PaperPriceHistory;
import com.inkcore.domain.paper.model.PaperSupplierPrice;

import java.util.List;

public interface PaperSupplierPriceRepositoryPort {

    List<PaperSupplierPrice> findByPaperId(String companyId, String paperId);

    List<PaperPriceHistory> findHistoryByPaperId(String companyId, String paperId);

    /**
     * Sincroniza la lista completa de precios del papel (insert/update/delete) y audita vía trigger.
     */
    List<PaperSupplierPrice> replaceDiff(
            String companyId,
            String paperId,
            List<PaperSupplierPrice> desiredPrices,
            String changedBy
    );
}
