package com.inkcore.domain.client.ports.out;

import com.inkcore.domain.client.model.Client;

import java.util.List;
import java.util.Optional;

public interface ClientRepositoryPort {

    Client save(Client client);

    Optional<Client> findById(String clientId);

    List<Client> findAll();

    List<Client> findAllByState(boolean state);

    List<Client> findAllByCompanyId(String companyId);

    List<Client> findAllByCompanyIdAndState(String companyId, boolean state);

    boolean existsByCompanyIdAndIdentificationIgnoreCase(String companyId, String identification);

    boolean existsByCompanyIdAndIdentificationIgnoreCaseExcludingClientId(
            String companyId,
            String identification,
            String clientId
    );
}
