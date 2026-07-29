package com.inkcore.application.client.usecase;

import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListClientsUseCase {

    private final ClientRepositoryPort clientRepository;

    public ListClientsUseCase(ClientRepositoryPort clientRepository) {
        this.clientRepository = clientRepository;
    }

    /**
     * @param companyId opcional; si se envía, filtra por empresa
     * @param state     {@code null} = todos; {@code true}/{@code false} = filtro por estado
     */
    @Transactional(readOnly = true)
    public PageResult<Client> execute(String companyId, Boolean state, PageQuery pageQuery) {
        PageQuery query = pageQuery == null ? PageQuery.of(0, PageQuery.DEFAULT_SIZE) : pageQuery;
        boolean filterCompany = companyId != null && !companyId.isBlank();
        if (filterCompany && state != null) {
            return clientRepository.findPageByCompanyIdAndState(companyId.trim(), state, query);
        }
        if (filterCompany) {
            return clientRepository.findPageByCompanyId(companyId.trim(), query);
        }
        if (state != null) {
            return clientRepository.findPageByState(state, query);
        }
        return clientRepository.findPage(query);
    }
}
