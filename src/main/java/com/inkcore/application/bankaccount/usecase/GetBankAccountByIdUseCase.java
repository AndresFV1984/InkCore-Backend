package com.inkcore.application.bankaccount.usecase;

import com.inkcore.domain.bankaccount.model.BankAccount;
import com.inkcore.domain.bankaccount.ports.out.BankAccountRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetBankAccountByIdUseCase {

    private final BankAccountRepositoryPort bankAccountRepository;

    public GetBankAccountByIdUseCase(BankAccountRepositoryPort bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    @Transactional(readOnly = true)
    public BankAccount execute(String accountId) {
        return bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "BANK_ACCOUNT_NOT_FOUND",
                        "Cuenta bancaria no encontrada"
                ));
    }
}
