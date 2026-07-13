package com.indicore.application.client.usecase;

import com.indicore.domain.client.model.Client;
import com.indicore.domain.client.ports.out.ClientRepositoryPort;
import com.indicore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetClientByIdUseCase {

    private final ClientRepositoryPort clientRepository;

    public GetClientByIdUseCase(ClientRepositoryPort clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public Client execute(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CLIENT_NOT_FOUND", "Cliente no encontrado"));
    }
}
