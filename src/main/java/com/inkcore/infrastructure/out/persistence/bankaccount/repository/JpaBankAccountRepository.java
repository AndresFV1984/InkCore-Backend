package com.inkcore.infrastructure.out.persistence.bankaccount.repository;

import com.inkcore.infrastructure.out.persistence.bankaccount.entity.BankAccountEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaBankAccountRepository extends JpaRepository<BankAccountEntity, String> {

    Page<BankAccountEntity> findAllByState(boolean state, Pageable pageable);

    Page<BankAccountEntity> findAllByCompanyId(String companyId, Pageable pageable);

    Page<BankAccountEntity> findAllByCompanyIdAndState(String companyId, boolean state, Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END
            FROM BankAccountEntity b
            WHERE b.companyId = :companyId
              AND LOWER(TRIM(b.accountNumber)) = LOWER(TRIM(:accountNumber))
            """)
    boolean existsByCompanyIdAndAccountNumberIgnoreCase(
            @Param("companyId") String companyId,
            @Param("accountNumber") String accountNumber
    );

    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN TRUE ELSE FALSE END
            FROM BankAccountEntity b
            WHERE b.companyId = :companyId
              AND LOWER(TRIM(b.accountNumber)) = LOWER(TRIM(:accountNumber))
              AND b.accountId <> :accountId
            """)
    boolean existsByCompanyIdAndAccountNumberIgnoreCaseAndAccountIdNot(
            @Param("companyId") String companyId,
            @Param("accountNumber") String accountNumber,
            @Param("accountId") String accountId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BankAccountEntity b
            SET b.primary = FALSE
            WHERE b.companyId = :companyId
              AND b.accountId <> :accountId
              AND b.primary = TRUE
            """)
    int clearPrimaryForCompanyExcept(
            @Param("companyId") String companyId,
            @Param("accountId") String accountId
    );
}
