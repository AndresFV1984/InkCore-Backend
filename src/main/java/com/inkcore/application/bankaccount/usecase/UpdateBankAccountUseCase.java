package com.inkcore.application.bankaccount.usecase;

import com.inkcore.domain.bankaccount.exception.BankAccountAlreadyExistsException;
import com.inkcore.domain.bankaccount.model.BankAccount;
import com.inkcore.domain.bankaccount.ports.out.BankAccountRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateBankAccountUseCase {

    private final BankAccountRepositoryPort bankAccountRepository;

    public UpdateBankAccountUseCase(BankAccountRepositoryPort bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    @Transactional
    public BankAccount execute(UpdateBankAccountCommand command) {
        BankAccount existing = bankAccountRepository.findById(command.accountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "BANK_ACCOUNT_NOT_FOUND",
                        "Cuenta bancaria no encontrada"
                ));

        String accountNumber = requireTrimmed(
                command.accountNumber(),
                "El número de cuenta es obligatorio"
        );
        if (bankAccountRepository.existsByCompanyIdAndAccountNumberIgnoreCaseExcludingAccountId(
                existing.getCompanyId(), accountNumber, existing.getAccountId())) {
            throw new BankAccountAlreadyExistsException("accountNumber", accountNumber);
        }

        BankAccount updated = existing.update(
                command.bankName(),
                command.accountType(),
                accountNumber,
                command.holderName(),
                command.holderNit(),
                command.includeInPdf(),
                command.primary(),
                command.state()
        );

        if (updated.isPrimary()) {
            bankAccountRepository.clearPrimaryForCompanyExcept(
                    updated.getCompanyId(),
                    updated.getAccountId()
            );
        }

        return bankAccountRepository.save(updated);
    }

    private static String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
