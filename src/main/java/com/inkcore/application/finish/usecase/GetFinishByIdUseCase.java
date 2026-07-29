package com.inkcore.application.finish.usecase;

import com.inkcore.domain.finish.model.Finish;
import com.inkcore.domain.finish.ports.out.FinishRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetFinishByIdUseCase {

    private final FinishRepositoryPort finishRepository;

    public GetFinishByIdUseCase(FinishRepositoryPort finishRepository) {
        this.finishRepository = finishRepository;
    }

    @Transactional(readOnly = true)
    public Finish execute(String finishId) {
        return finishRepository.findById(finishId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FINISH_NOT_FOUND",
                        "Terminado no encontrado"
                ));
    }
}
