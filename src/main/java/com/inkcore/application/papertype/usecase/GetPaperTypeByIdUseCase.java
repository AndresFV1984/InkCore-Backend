package com.inkcore.application.papertype.usecase;

import com.inkcore.domain.papertype.model.PaperType;
import com.inkcore.domain.papertype.ports.out.PaperTypeRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetPaperTypeByIdUseCase {

    private final PaperTypeRepositoryPort paperTypeRepository;

    public GetPaperTypeByIdUseCase(PaperTypeRepositoryPort paperTypeRepository) {
        this.paperTypeRepository = paperTypeRepository;
    }

    @Transactional(readOnly = true)
    public PaperType execute(String paperTypeId) {
        return paperTypeRepository.findById(paperTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "PAPER_TYPE_NOT_FOUND",
                        "Tipo de papel no encontrado"
                ));
    }
}
