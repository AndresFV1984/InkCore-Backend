package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.model.CxcDocumentConcept;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListOrderCxcDocumentsUseCase {

    private final OrderSupport support;
    private final OrderCxcDocumentService cxcDocumentService;

    public ListOrderCxcDocumentsUseCase(
            OrderSupport support,
            OrderCxcDocumentService cxcDocumentService
    ) {
        this.support = support;
        this.cxcDocumentService = cxcDocumentService;
    }

    @Transactional(readOnly = true)
    public List<OrderCxcDocumentService.OpenCxcView> execute(
            String productionOrderId,
            String concept,
            Authentication authentication
    ) {
        return execute(productionOrderId, concept, true, authentication);
    }

    @Transactional(readOnly = true)
    public List<OrderCxcDocumentService.OpenCxcView> execute(
            String productionOrderId,
            String concept,
            boolean openOnly,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        support.requireActiveOrder(productionOrderId, companyId);
        CxcDocumentConcept filter = (concept == null || concept.isBlank())
                ? null
                : CxcDocumentConcept.fromValue(concept);
        return cxcDocumentService.listWithRemaining(companyId, productionOrderId, filter, openOnly);
    }
}
