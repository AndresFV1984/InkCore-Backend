package com.inkcore.infrastructure.out.persistence.bankaccount.adapter;

import com.inkcore.domain.bankaccount.model.BankAccount;
import com.inkcore.domain.bankaccount.ports.out.BankAccountRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.infrastructure.out.persistence.bankaccount.entity.BankAccountEntity;
import com.inkcore.infrastructure.out.persistence.bankaccount.mapper.BankAccountPersistenceMapper;
import com.inkcore.infrastructure.out.persistence.bankaccount.repository.JpaBankAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class BankAccountPersistenceAdapter implements BankAccountRepositoryPort {

    private final JpaBankAccountRepository jpaBankAccountRepository;
    private final BankAccountPersistenceMapper mapper;

    public BankAccountPersistenceAdapter(
            JpaBankAccountRepository jpaBankAccountRepository,
            BankAccountPersistenceMapper mapper
    ) {
        this.jpaBankAccountRepository = jpaBankAccountRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public BankAccount save(BankAccount bankAccount) {
        BankAccountEntity existing = jpaBankAccountRepository.findById(bankAccount.getAccountId()).orElse(null);
        if (existing == null) {
            BankAccountEntity entity = mapper.toNewEntity(bankAccount);
            BankAccountEntity saved = jpaBankAccountRepository.save(entity);
            return mapper.toDomain(saved);
        }
        mapper.copyScalars(bankAccount, existing);
        BankAccountEntity saved = jpaBankAccountRepository.save(existing);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BankAccount> findById(String accountId) {
        return jpaBankAccountRepository.findById(accountId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BankAccount> findPage(PageQuery pageQuery) {
        return mapPage(jpaBankAccountRepository.findAll(pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BankAccount> findPageByState(boolean state, PageQuery pageQuery) {
        return mapPage(jpaBankAccountRepository.findAllByState(state, pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BankAccount> findPageByCompanyId(String companyId, PageQuery pageQuery) {
        return mapPage(jpaBankAccountRepository.findAllByCompanyId(companyId, pageable(pageQuery)), pageQuery);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<BankAccount> findPageByCompanyIdAndState(String companyId, boolean state, PageQuery pageQuery) {
        return mapPage(
                jpaBankAccountRepository.findAllByCompanyIdAndState(companyId, state, pageable(pageQuery)),
                pageQuery
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndAccountNumberIgnoreCase(String companyId, String accountNumber) {
        return companyId != null
                && accountNumber != null
                && jpaBankAccountRepository.existsByCompanyIdAndAccountNumberIgnoreCase(
                companyId.trim(), accountNumber.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndAccountNumberIgnoreCaseExcludingAccountId(
            String companyId,
            String accountNumber,
            String accountId
    ) {
        return companyId != null
                && accountNumber != null
                && accountId != null
                && jpaBankAccountRepository.existsByCompanyIdAndAccountNumberIgnoreCaseAndAccountIdNot(
                companyId.trim(), accountNumber.trim(), accountId);
    }

    @Override
    @Transactional
    public void clearPrimaryForCompanyExcept(String companyId, String accountId) {
        if (companyId == null || accountId == null) {
            return;
        }
        jpaBankAccountRepository.clearPrimaryForCompanyExcept(companyId.trim(), accountId);
    }

    private static PageRequest pageable(PageQuery pageQuery) {
        return PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by(
                Sort.Order.desc("primary"),
                Sort.Order.asc("bankName"),
                Sort.Order.asc("accountNumber")
        ));
    }

    private PageResult<BankAccount> mapPage(Page<BankAccountEntity> page, PageQuery pageQuery) {
        return new PageResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                pageQuery.page(),
                pageQuery.size(),
                page.getTotalElements()
        );
    }
}
