package com.inkcore.application.bankaccount.usecase;

import com.inkcore.domain.bankaccount.exception.BankAccountAlreadyExistsException;
import com.inkcore.domain.bankaccount.model.BankAccount;
import com.inkcore.domain.bankaccount.ports.out.BankAccountRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

@Service
public class CreateBankAccountUseCase {

    private final BankAccountRepositoryPort bankAccountRepository;
    private final Clock clock;

    public CreateBankAccountUseCase(BankAccountRepositoryPort bankAccountRepository, Clock clock) {
        this.bankAccountRepository = bankAccountRepository;
        this.clock = clock;
    }

    @Transactional
    public BankAccount execute(CreateBankAccountCommand command) {
        String accountNumber = requireTrimmed(command.accountNumber(), "El número de cuenta es obligatorio");
        if (bankAccountRepository.existsByCompanyIdAndAccountNumberIgnoreCase(
                command.companyId(), accountNumber)) {
            throw new BankAccountAlreadyExistsException("accountNumber", accountNumber);
        }

        boolean includeInPdf = Objects.requireNonNullElse(command.includeInPdf(), true);
        boolean primary = Objects.requireNonNullElse(command.primary(), false);
        boolean state = Objects.requireNonNullElse(command.state(), true);

        BankAccount account = BankAccount.createNew(
                command.companyId(),
                command.bankName(),
                command.accountType(),
                accountNumber,
                command.holderName(),
                command.holderNit(),
                includeInPdf,
                primary,
                state,
                LocalDate.now(clock)
        );

        if (account.isPrimary()) {
            bankAccountRepository.clearPrimaryForCompanyExcept(account.getCompanyId(), account.getAccountId());
        }

        return bankAccountRepository.save(account);
    }

    private static String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
