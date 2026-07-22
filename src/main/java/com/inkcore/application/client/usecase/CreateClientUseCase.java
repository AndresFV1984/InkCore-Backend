package com.inkcore.application.client.usecase;

import com.inkcore.domain.client.exception.ClientAlreadyExistsException;
import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

@Service
public class CreateClientUseCase {

    private final ClientRepositoryPort clientRepository;
    private final Clock clock;

    public CreateClientUseCase(ClientRepositoryPort clientRepository, Clock clock) {
        this.clientRepository = clientRepository;
        this.clock = clock;
    }

    @Transactional
    public Client execute(CreateClientCommand command) {
        String identification = blankToNull(command.identification());
        if (identification != null
                && clientRepository.existsByCompanyIdAndIdentificationIgnoreCase(
                command.companyId(), identification)) {
            throw new ClientAlreadyExistsException("identification", identification);
        }

        boolean effectiveState = Objects.requireNonNullElse(command.state(), true);
        Client client = Client.createNew(
                command.companyId(),
                command.name(),
                command.documentType(),
                identification,
                command.department(),
                command.city(),
                command.address(),
                command.phone(),
                command.email(),
                command.contactPerson(),
                effectiveState,
                LocalDate.now(clock)
        );
        return clientRepository.save(client);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
