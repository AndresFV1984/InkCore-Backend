package com.indicore.infrastructure.out.persistence.client.adapter;

import com.indicore.domain.client.model.Client;
import com.indicore.domain.client.ports.out.ClientRepositoryPort;
import com.indicore.infrastructure.out.persistence.client.mapper.ClientPersistenceMapper;
import com.indicore.infrastructure.out.persistence.client.repository.JpaClientRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ClientPersistenceAdapter implements ClientRepositoryPort {

    private final JpaClientRepository jpaClientRepository;
    private final ClientPersistenceMapper mapper;

    public ClientPersistenceAdapter(JpaClientRepository jpaClientRepository, ClientPersistenceMapper mapper) {
        this.jpaClientRepository = jpaClientRepository;
        this.mapper = mapper;
    }

    @Override
    public Client save(Client client) {
        var saved = jpaClientRepository.save(mapper.toEntity(client));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Client> findById(UUID id) {
        return jpaClientRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Client> findAll() {
        return jpaClientRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByNit(String nit) {
        if (nit == null || nit.isBlank()) {
            return false;
        }
        return jpaClientRepository.existsByNitIgnoreCase(nit.trim());
    }
}
