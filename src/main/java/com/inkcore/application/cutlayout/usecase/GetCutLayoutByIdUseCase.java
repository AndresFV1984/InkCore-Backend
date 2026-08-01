package com.inkcore.application.cutlayout.usecase;

import com.inkcore.domain.cutlayout.model.CutLayout;
import com.inkcore.domain.cutlayout.ports.out.CutLayoutRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetCutLayoutByIdUseCase {

    private final CutLayoutRepositoryPort cutLayoutRepository;

    public GetCutLayoutByIdUseCase(CutLayoutRepositoryPort cutLayoutRepository) {
        this.cutLayoutRepository = cutLayoutRepository;
    }

    @Transactional(readOnly = true)
    public CutLayout execute(String cutLayoutId) {
        return cutLayoutRepository.findById(cutLayoutId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CUT_LAYOUT_NOT_FOUND",
                        "Despiece no encontrado"
                ));
    }
}
