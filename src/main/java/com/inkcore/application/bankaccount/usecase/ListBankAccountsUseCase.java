package com.inkcore.application.bankaccount.usecase;

import com.inkcore.domain.bankaccount.model.BankAccount;
import com.inkcore.domain.bankaccount.ports.out.BankAccountRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListBankAccountsUseCase {

    private final BankAccountRepositoryPort bankAccountRepository;

    public ListBankAccountsUseCase(BankAccountRepositoryPort bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    /**
     * @param companyId opcional; si se envía, filtra por empresa
     * @param state     {@code null} = todos; {@code true}/{@code false} = filtro por estado
     */
    @Transactional(readOnly = true)
    public PageResult<BankAccount> execute(String companyId, Boolean state, PageQuery pageQuery) {
        PageQuery query = pageQuery == null ? PageQuery.of(0, PageQuery.DEFAULT_SIZE) : pageQuery;
        boolean filterCompany = companyId != null && !companyId.isBlank();
        if (filterCompany && state != null) {
            return bankAccountRepository.findPageByCompanyIdAndState(companyId.trim(), state, query);
        }
        if (filterCompany) {
            return bankAccountRepository.findPageByCompanyId(companyId.trim(), query);
        }
        if (state != null) {
            return bankAccountRepository.findPageByState(state, query);
        }
        return bankAccountRepository.findPage(query);
    }
}
