package com.inkcore.application.client.usecase;

import com.inkcore.domain.client.exception.ClientAlreadyExistsException;
import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateClientUseCase {

    private final ClientRepositoryPort clientRepository;

    public UpdateClientUseCase(ClientRepositoryPort clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional
    public Client execute(UpdateClientCommand command) {
        Client existing = clientRepository.findById(command.clientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CLIENT_NOT_FOUND",
                        "Cliente no encontrado"
                ));

        String identification = blankToNull(command.identification());
        if (identification != null
                && clientRepository.existsByCompanyIdAndIdentificationIgnoreCaseExcludingClientId(
                existing.getCompanyId(), identification, existing.getClientId())) {
            throw new ClientAlreadyExistsException("identification", identification);
        }

        Client updated = existing.update(
                command.name(),
                command.documentType(),
                identification,
                command.department(),
                command.city(),
                command.address(),
                command.phone(),
                command.email(),
                command.contactPerson(),
                command.state()
        );
        return clientRepository.save(updated);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
