package com.inkcore.domain.order.ports.out;

import com.inkcore.domain.order.model.NegotiatedAddition;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface NegotiatedAdditionRepositoryPort {

    NegotiatedAddition save(NegotiatedAddition addition);

    /** Actualiza un registro ya persistido. No inserta un ADN nuevo. */
    NegotiatedAddition update(NegotiatedAddition addition);

    Optional<NegotiatedAddition> findByIdForUpdate(String companyId, String orderNegotiatedAdditionId);

    List<NegotiatedAddition> findByProductionOrderId(String companyId, String productionOrderId);

    boolean existsReversionFor(String companyId, String reversedAdditionId);

    /** Suma de ADN vigentes (voided_at nulo). 0.00 si la OP no tiene adicionales. */
    BigDecimal sumNetAmount(String companyId, String productionOrderId);

    /** Suma de ADN vigentes por OP (una sola consulta). OPs sin ADN no aparecen en el mapa. */
    Map<String, BigDecimal> sumNetAmountByOrderIds(String companyId, Collection<String> productionOrderIds);

    long allocateNextSequence(String companyId);
}
