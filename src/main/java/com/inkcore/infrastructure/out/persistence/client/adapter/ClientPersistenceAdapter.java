package com.inkcore.infrastructure.out.persistence.client.adapter;

import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.infrastructure.out.persistence.client.entity.ClientEntity;
import com.inkcore.infrastructure.out.persistence.client.mapper.ClientPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.client.repository.JpaClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class ClientPersistenceAdapter implements ClientRepositoryPort {

    private final JpaClientRepository jpaClientRepository;
    private final ClientPersistenceMapper mapper;

    public ClientPersistenceAdapter(
            JpaClientRepository jpaClientRepository,
            ClientPersistenceMapper mapper
    ) {
        this.jpaClientRepository = jpaClientRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Client save(Client client) {
        ClientEntity existing = jpaClientRepository.findById(client.getClientId()).orElse(null);
        if (existing == null) {
            ClientEntity entity = mapper.toNewEntity(client);
            ClientEntity saved = jpaClientRepository.save(entity);
            return mapper.toDomain(saved);
        }
        mapper.copyScalars(client, existing);
        ClientEntity saved = jpaClientRepository.save(existing);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Client> findById(String clientId) {
        return jpaClientRepository.findById(clientId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Client> findAll() {
        return jpaClientRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Client> findAllByState(boolean state) {
        return jpaClientRepository.findAllByState(state).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Client> findAllByCompanyId(String companyId) {
        return jpaClientRepository.findAllByCompanyId(companyId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Client> findAllByCompanyIdAndState(String companyId, boolean state) {
        return jpaClientRepository.findAllByCompanyIdAndState(companyId, state).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndIdentificationIgnoreCase(String companyId, String identification) {
        return companyId != null
                && identification != null
                && jpaClientRepository.existsByCompanyIdAndIdentificationIgnoreCase(
                companyId.trim(), identification.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndIdentificationIgnoreCaseExcludingClientId(
            String companyId,
            String identification,
            String clientId
    ) {
        return companyId != null
                && identification != null
                && clientId != null
                && jpaClientRepository.existsByCompanyIdAndIdentificationIgnoreCaseAndClientIdNot(
                companyId.trim(), identification.trim(), clientId);
    }
}
