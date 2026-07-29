package com.inkcore.application.bankaccount.usecase;

import com.inkcore.domain.bankaccount.exception.BankAccountAlreadyExistsException;
import com.inkcore.domain.bankaccount.model.BankAccount;
import com.inkcore.domain.bankaccount.ports.out.BankAccountRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateBankAccountUseCaseTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-07-28T12:00:00Z");

    @Mock BankAccountRepositoryPort bankAccountRepository;

    private CreateBankAccountUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateBankAccountUseCase(
                bankAccountRepository,
                Clock.fixed(FIXED_NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void execute_success_defaultsIncludeInPdfAndStateTrue() {
        when(bankAccountRepository.existsByCompanyIdAndAccountNumberIgnoreCase(
                "company-seed-001", "12345678901")).thenReturn(false);
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        BankAccount created = useCase.execute(new CreateBankAccountCommand(
                "company-seed-001",
                "Bancolombia",
                "corriente",
                "12345678901",
                "InkCore S.A.S.",
                "900.000.000-1",
                null,
                null,
                null
        ));

        assertEquals("Bancolombia", created.getBankName());
        assertEquals("Corriente", created.getAccountType());
        assertEquals("12345678901", created.getAccountNumber());
        assertEquals("InkCore S.A.S.", created.getHolderName());
        assertEquals("900.000.000-1", created.getHolderNit());
        assertTrue(created.isIncludeInPdf());
        assertFalse(created.isPrimary());
        assertTrue(created.isState());
        assertEquals(LocalDate.of(2026, 7, 28), created.getCreationDate());

        ArgumentCaptor<BankAccount> captor = ArgumentCaptor.forClass(BankAccount.class);
        verify(bankAccountRepository).save(captor.capture());
        assertEquals("company-seed-001", captor.getValue().getCompanyId());
        verify(bankAccountRepository, never()).clearPrimaryForCompanyExcept(anyString(), anyString());
    }

    @Test
    void execute_primary_clearsOtherPrimary() {
        when(bankAccountRepository.existsByCompanyIdAndAccountNumberIgnoreCase(
                "company-seed-001", "12345678901")).thenReturn(false);
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        BankAccount created = useCase.execute(new CreateBankAccountCommand(
                "company-seed-001",
                "Bancolombia",
                "Ahorros",
                "12345678901",
                "InkCore S.A.S.",
                null,
                true,
                true,
                true
        ));

        assertTrue(created.isPrimary());
        verify(bankAccountRepository).clearPrimaryForCompanyExcept(
                eq("company-seed-001"),
                eq(created.getAccountId())
        );
    }

    @Test
    void execute_duplicateAccountNumber_throwsConflict() {
        when(bankAccountRepository.existsByCompanyIdAndAccountNumberIgnoreCase(
                "company-seed-001", "12345678901")).thenReturn(true);

        assertThrows(BankAccountAlreadyExistsException.class, () -> useCase.execute(new CreateBankAccountCommand(
                "company-seed-001",
                "Bancolombia",
                "Corriente",
                "12345678901",
                "InkCore S.A.S.",
                null,
                true,
                false,
                true
        )));

        verify(bankAccountRepository, never()).save(any());
    }
}
