package com.inkcore.infrastructure.out.persistence.order.repository;

import com.inkcore.infrastructure.out.persistence.order.entity.NegotiatedAdditionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface JpaNegotiatedAdditionRepository extends JpaRepository<NegotiatedAdditionEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<NegotiatedAdditionEntity> findForUpdateByCompanyIdAndOrderNegotiatedAdditionId(
            String companyId,
            String orderNegotiatedAdditionId
    );

    List<NegotiatedAdditionEntity> findAllByCompanyIdAndProductionOrderIdOrderByApprovedAtDescCreatedAtDesc(
            String companyId,
            String productionOrderId
    );

    boolean existsByCompanyIdAndOrderNegotiatedAdditionIdAndVoidedAtIsNotNull(
            String companyId,
            String orderNegotiatedAdditionId
    );

    boolean existsByCompanyIdAndReversedAdditionIdAndMovementType(
            String companyId,
            String reversedAdditionId,
            String movementType
    );
}
