package com.indicore.application.remission.usecase;

import com.indicore.domain.remission.model.Remission;
import com.indicore.domain.remission.ports.out.RemissionRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListRemissionsUseCase {

    private final RemissionRepositoryPort remissionRepository;

    public ListRemissionsUseCase(RemissionRepositoryPort remissionRepository) {
        this.remissionRepository = remissionRepository;
    }

    @Transactional(readOnly = true)
    public List<Remission> execute() {
        return remissionRepository.findAll();
    }
}
