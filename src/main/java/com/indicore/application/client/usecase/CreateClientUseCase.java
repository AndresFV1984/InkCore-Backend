package com.indicore.application.client.usecase;

import com.indicore.domain.client.exception.ClientAlreadyExistsException;
import com.indicore.domain.client.model.Client;
import com.indicore.domain.client.ports.out.ClientRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateClientUseCase {

    private final ClientRepositoryPort clientRepository;

    public CreateClientUseCase(ClientRepositoryPort clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional
    public Client execute(CreateClientCommand command) {
        if (command.nit() != null && !command.nit().isBlank()
                && clientRepository.existsByNit(command.nit().trim())) {
            throw new ClientAlreadyExistsException(command.nit().trim());
        }

        Client client = Client.createNew(
                command.name(),
                command.nit(),
                command.phone(),
                command.city(),
                command.address(),
                command.email(),
                command.contact()
        );

        return clientRepository.save(client);
    }
}
