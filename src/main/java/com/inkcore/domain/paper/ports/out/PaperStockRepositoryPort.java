package com.inkcore.domain.paper.ports.out;

import com.inkcore.domain.paper.model.PaperStock;

import java.util.List;
import java.util.Optional;

public interface PaperStockRepositoryPort {

    PaperStock save(PaperStock stock);

    Optional<PaperStock> findById(String paperStockId);

    List<PaperStock> findByPaperId(String companyId, String paperId, Boolean state);

    void deleteById(String paperStockId);
}
