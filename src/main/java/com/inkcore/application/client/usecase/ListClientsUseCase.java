package com.inkcore.application.client.usecase;

import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public List<Client> execute(String companyId, Boolean state) {
        boolean filterCompany = companyId != null && !companyId.isBlank();
        if (filterCompany && state != null) {
            return clientRepository.findAllByCompanyIdAndState(companyId.trim(), state);
        }
        if (filterCompany) {
            return clientRepository.findAllByCompanyId(companyId.trim());
        }
        if (state != null) {
            return clientRepository.findAllByState(state);
        }
        return clientRepository.findAll();
    }
}
