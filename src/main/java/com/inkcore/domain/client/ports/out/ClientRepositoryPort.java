package com.inkcore.domain.client.ports.out;

import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.client.model.Client;

import java.util.Optional;

public interface ClientRepositoryPort {

    Client save(Client client);

    Optional<Client> findById(String clientId);

    PageResult<Client> findPage(PageQuery pageQuery);

    PageResult<Client> findPageByState(boolean state, PageQuery pageQuery);

    PageResult<Client> findPageByCompanyId(String companyId, PageQuery pageQuery);

    PageResult<Client> findPageByCompanyIdAndState(String companyId, boolean state, PageQuery pageQuery);

    boolean existsByCompanyIdAndIdentificationIgnoreCase(String companyId, String identification);

    boolean existsByCompanyIdAndIdentificationIgnoreCaseExcludingClientId(
            String companyId,
            String identification,
            String clientId
    );
}
