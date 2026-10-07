package com.inkcore.domain.paper.ports.out;

import com.inkcore.domain.paper.model.PaperRemnant;

import java.util.List;
import java.util.Optional;

public interface PaperRemnantRepositoryPort {

    PaperRemnant save(PaperRemnant remnant);

    Optional<PaperRemnant> findById(String paperRemnantId);

    List<PaperRemnant> findByPaperId(String companyId, String paperId, Boolean state);

    void deleteById(String paperRemnantId);
}
