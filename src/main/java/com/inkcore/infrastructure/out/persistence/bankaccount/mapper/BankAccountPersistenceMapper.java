package com.inkcore.infrastructure.out.persistence.bankaccount.mapper;

import com.inkcore.domain.bankaccount.model.BankAccount;
import com.inkcore.infrastructure.out.persistence.bankaccount.entity.BankAccountEntity;
import org.springframework.stereotype.Component;

@Component
public class BankAccountPersistenceMapper {

    public BankAccountEntity toNewEntity(BankAccount account) {
        BankAccountEntity e = new BankAccountEntity();
        copyScalars(account, e);
        return e;
    }

    public void copyScalars(BankAccount account, BankAccountEntity e) {
        e.setAccountId(account.getAccountId());
        e.setCompanyId(account.getCompanyId());
        e.setBankName(account.getBankName());
        e.setAccountType(account.getAccountType());
        e.setAccountNumber(account.getAccountNumber());
        e.setHolderName(account.getHolderName());
        e.setHolderNit(blankToNull(account.getHolderNit()));
        e.setIncludeInPdf(account.isIncludeInPdf());
        e.setPrimary(account.isPrimary());
        e.setState(account.isState());
        e.setCreationDate(account.getCreationDate());
    }

    public BankAccount toDomain(BankAccountEntity entity) {
        return BankAccount.reconstitute(
                entity.getAccountId(),
                entity.getCompanyId(),
                entity.getBankName(),
                entity.getAccountType(),
                entity.getAccountNumber(),
                entity.getHolderName(),
                entity.getHolderNit() == null ? "" : entity.getHolderNit(),
                entity.isIncludeInPdf(),
                entity.isPrimary(),
                entity.isState(),
                entity.getCreationDate()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
