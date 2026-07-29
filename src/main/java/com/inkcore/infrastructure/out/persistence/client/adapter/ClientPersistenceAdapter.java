package com.inkcore.infrastructure.out.persistence.client.adapter;

import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.infrastructure.out.persistence.client.entity.ClientEntity;
import com.inkcore.infrastructure.out.persistence.client.mapper.ClientPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.client.repository.JpaClientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    public PageResult<Client> findPage(PageQuery pageQuery) {
        return mapPage(jpaClientRepository.findAll(pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Client> findPageByState(boolean state, PageQuery pageQuery) {
        return mapPage(jpaClientRepository.findAllByState(state, pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Client> findPageByCompanyId(String companyId, PageQuery pageQuery) {
        return mapPage(jpaClientRepository.findAllByCompanyId(companyId, pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Client> findPageByCompanyIdAndState(String companyId, boolean state, PageQuery pageQuery) {
        return mapPage(
                jpaClientRepository.findAllByCompanyIdAndState(companyId, state, pageable(pageQuery)),
                pageQuery
        );
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

    private static PageRequest pageable(PageQuery pageQuery) {
        return PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by(Sort.Direction.ASC, "name"));
    }

    private PageResult<Client> mapPage(Page<ClientEntity> page, PageQuery pageQuery) {
        return new PageResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                pageQuery.page(),
                pageQuery.size(),
                page.getTotalElements()
        );
    }
}
