package com.inkcore.domain.bankaccount.ports.out;

import com.inkcore.domain.bankaccount.model.BankAccount;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;

import java.util.Optional;

public interface BankAccountRepositoryPort {

    BankAccount save(BankAccount bankAccount);

    Optional<BankAccount> findById(String accountId);

    PageResult<BankAccount> findPage(PageQuery pageQuery);

    PageResult<BankAccount> findPageByState(boolean state, PageQuery pageQuery);

    PageResult<BankAccount> findPageByCompanyId(String companyId, PageQuery pageQuery);

    PageResult<BankAccount> findPageByCompanyIdAndState(String companyId, boolean state, PageQuery pageQuery);

    boolean existsByCompanyIdAndAccountNumberIgnoreCase(String companyId, String accountNumber);

    boolean existsByCompanyIdAndAccountNumberIgnoreCaseExcludingAccountId(
            String companyId,
            String accountNumber,
            String accountId
    );

    /**
     * Quita la marca de principal a otras cuentas de la compañía (excepto {@code accountId}).
     */
    void clearPrimaryForCompanyExcept(String companyId, String accountId);
}
