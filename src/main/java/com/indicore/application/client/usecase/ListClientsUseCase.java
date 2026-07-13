package com.indicore.application.client.usecase;

import com.indicore.domain.client.model.Client;
import com.indicore.domain.client.ports.out.ClientRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListClientsUseCase {

    private final ClientRepositoryPort clientRepository;

    public ListClientsUseCase(ClientRepositoryPort clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<Client> execute() {
        return clientRepository.findAll();
    }
}
