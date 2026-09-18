package com.inkcore.infrastructure.out.persistence.order.repository;

import com.inkcore.infrastructure.out.persistence.order.entity.AccountsReceivableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface JpaAccountsReceivableRepository
        extends JpaRepository<AccountsReceivableEntity, String>, JpaSpecificationExecutor<AccountsReceivableEntity> {

    Optional<AccountsReceivableEntity> findByCompanyIdAndProductionOrderId(String companyId, String productionOrderId);

    List<AccountsReceivableEntity> findAllByCompanyIdAndClientIdOrderByUpdatedAtDesc(
            String companyId,
            String clientId
    );
}
