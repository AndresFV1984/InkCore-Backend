package com.inkcore.domain.order.ports.out;

import com.inkcore.domain.order.model.CxcDocumentConcept;
import com.inkcore.domain.order.model.OrderCxcDocument;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderCxcDocumentRepositoryPort {

    OrderCxcDocument save(OrderCxcDocument document);

    OrderCxcDocument update(OrderCxcDocument document);

    Optional<OrderCxcDocument> findById(String companyId, String cxcDocumentId);

    Optional<OrderCxcDocument> findByIdForUpdate(String companyId, String cxcDocumentId);

    Optional<OrderCxcDocument> findActiveByDeliveryId(String companyId, String orderDeliveryId);

    Optional<OrderCxcDocument> findActiveByAdditionId(String companyId, String orderNegotiatedAdditionId);

    List<OrderCxcDocument> findByProductionOrderId(String companyId, String productionOrderId);

    /** Documentos de cartera no anulados de la compañía (dashboard CxC: 1 fila = 1 CxC). */
    List<OrderCxcDocument> findActiveByCompanyId(String companyId);

    List<OrderCxcDocument> findOpenByProductionOrderAndConcept(
            String companyId,
            String productionOrderId,
            CxcDocumentConcept concept
    );

    /** Suma de aplicaciones activas a la CxC. */
    BigDecimal sumApplied(String companyId, String cxcDocumentId);

    long countByProductionOrderId(String companyId, String productionOrderId);

    String allocateNextCxcNumber(String companyId);
}
