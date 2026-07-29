package com.inkcore.application.finishingprocess.usecase;

import com.inkcore.domain.finishingprocess.model.FinishingProcess;
import com.inkcore.domain.finishingprocess.ports.out.FinishingProcessRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetFinishingProcessByIdUseCase {

    private final FinishingProcessRepositoryPort finishingProcessRepository;

    public GetFinishingProcessByIdUseCase(FinishingProcessRepositoryPort finishingProcessRepository) {
        this.finishingProcessRepository = finishingProcessRepository;
    }

    @Transactional(readOnly = true)
    public FinishingProcess execute(String finishingProcessId) {
        return finishingProcessRepository.findById(finishingProcessId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "FINISHING_PROCESS_NOT_FOUND",
                        "Proceso de acabado no encontrado"
                ));
    }
}
