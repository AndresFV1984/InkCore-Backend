package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.order.model.CxcDocumentConcept;
import com.inkcore.domain.order.model.CxcDocumentStatus;
import com.inkcore.domain.order.model.CustomerOrder;
import com.inkcore.domain.order.model.NegotiatedAddition;
import com.inkcore.domain.order.model.OrderCxcDocument;
import com.inkcore.domain.order.ports.out.NegotiatedAdditionRepositoryPort;
import com.inkcore.domain.order.ports.out.OrderCxcDocumentRepositoryPort;
import com.inkcore.domain.order.ports.out.OrderDeliveryRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dashboard CxC: 1 fila = 1 documento de cartera (ENT o ADN), no 1 fila por OP.
 */
@Service
public class ListCxcDocumentsDashboardUseCase {

    private final OrderSupport support;
    private final OrderCxcDocumentRepositoryPort cxcDocumentRepository;
    private final OrderDeliveryRepositoryPort deliveryRepository;
    private final NegotiatedAdditionRepositoryPort additionRepository;
    private final ClientRepositoryPort clientRepository;

    public ListCxcDocumentsDashboardUseCase(
            OrderSupport support,
            OrderCxcDocumentRepositoryPort cxcDocumentRepository,
            OrderDeliveryRepositoryPort deliveryRepository,
            NegotiatedAdditionRepositoryPort additionRepository,
            ClientRepositoryPort clientRepository
    ) {
        this.support = support;
        this.cxcDocumentRepository = cxcDocumentRepository;
        this.deliveryRepository = deliveryRepository;
        this.additionRepository = additionRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<CxcDocumentDashboardRow> execute(
            String concept,
            String clientId,
            String search,
            Boolean overdueOnly,
            Boolean dueSoonOnly,
            Boolean withBalance,
            PageQuery pageQuery,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        PageQuery query = pageQuery == null ? PageQuery.of(0, PageQuery.DEFAULT_SIZE) : pageQuery;
        LocalDate today = LocalDate.now();

        CxcDocumentConcept conceptFilter = parseConcept(concept);
        List<OrderCxcDocument> docs = cxcDocumentRepository.findActiveByCompanyId(companyId);
        if (conceptFilter != null) {
            docs = docs.stream().filter(d -> d.getConcept() == conceptFilter).toList();
        }
        if (clientId != null && !clientId.isBlank()) {
            String cid = clientId.trim();
            docs = docs.stream().filter(d -> cid.equals(d.getClientId())).toList();
        }

        Set<String> productionOrderIds = docs.stream()
                .map(OrderCxcDocument::getProductionOrderId)
                .collect(Collectors.toCollection(HashSet::new));
        Map<String, CustomerOrder> customerOrderByOp = support.customerOrderRepository()
                .findByProductionOrderIds(companyId, productionOrderIds)
                .stream()
                .collect(Collectors.toMap(CustomerOrder::getProductionOrderId, co -> co, (a, b) -> a));

        Map<String, String> orderNumberByOp = new HashMap<>();
        for (String opId : productionOrderIds) {
            support.productionOrderRepository().findById(opId).ifPresent(order ->
                    orderNumberByOp.put(opId, order.getOrderNumber())
            );
        }

        Map<String, String> clientNameById = new HashMap<>();
        for (OrderCxcDocument doc : docs) {
            String cid = doc.getClientId();
            if (cid == null || clientNameById.containsKey(cid)) continue;
            clientNameById.put(cid, clientRepository.findById(cid).map(Client::getName).orElse(null));
        }

        Map<String, String> deliveryNumberById = new HashMap<>();
        for (OrderCxcDocument doc : docs) {
            if (doc.getConcept() != CxcDocumentConcept.ENTREGA) continue;
            String deliveryId = doc.getSourceDeliveryId();
            if (deliveryId == null || deliveryId.isBlank() || deliveryNumberById.containsKey(deliveryId)) {
                continue;
            }
            deliveryRepository.findById(companyId, deliveryId).ifPresent(delivery ->
                    deliveryNumberById.put(deliveryId, delivery.getDeliveryNumber())
            );
        }

        Map<String, String> additionNumberById = new HashMap<>();
        Set<String> opsWithExtra = docs.stream()
                .filter(d -> d.getConcept() == CxcDocumentConcept.EXTRA)
                .map(OrderCxcDocument::getProductionOrderId)
                .collect(Collectors.toCollection(HashSet::new));
        for (String opId : opsWithExtra) {
            for (NegotiatedAddition addition : additionRepository.findByProductionOrderId(companyId, opId)) {
                additionNumberById.put(
                        addition.getOrderNegotiatedAdditionId(),
                        addition.getAdditionNumber()
                );
            }
        }

        List<CxcDocumentDashboardRow> rows = new ArrayList<>(docs.size());
        for (OrderCxcDocument doc : docs) {
            BigDecimal applied = cxcDocumentRepository.sumApplied(companyId, doc.getCxcDocumentId())
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal remaining = doc.getTotal().subtract(applied).setScale(2, RoundingMode.HALF_UP);
            if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                remaining = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }

            String sourceNumber = null;
            if (doc.getConcept() == CxcDocumentConcept.ENTREGA) {
                sourceNumber = deliveryNumberById.get(doc.getSourceDeliveryId());
            } else if (doc.getConcept() == CxcDocumentConcept.EXTRA) {
                sourceNumber = additionNumberById.get(doc.getSourceAdditionId());
            }

            CustomerOrder customerOrder = customerOrderByOp.get(doc.getProductionOrderId());
            AgingBits aging = agingOf(doc.getDueDate(), remaining, today);

            rows.add(new CxcDocumentDashboardRow(
                    doc.getCxcDocumentId(),
                    doc.getCxcNumber(),
                    doc.getProductionOrderId(),
                    orderNumberByOp.get(doc.getProductionOrderId()),
                    customerOrder == null ? null : customerOrder.getOdpNumber(),
                    doc.getClientId(),
                    clientNameById.get(doc.getClientId()),
                    doc.getConcept() == null
                            ? CxcDocumentConcept.ENTREGA.getDbValue()
                            : doc.getConcept().getDbValue(),
                    doc.getSourceDeliveryId(),
                    doc.getSourceAdditionId(),
                    sourceNumber,
                    doc.getTotal().setScale(2, RoundingMode.HALF_UP),
                    applied,
                    remaining,
                    doc.getStatus() == null ? CxcDocumentStatus.PENDIENTE.getDbValue() : doc.getStatus().getDbValue(),
                    doc.getIssuedAt(),
                    doc.getDueDate(),
                    doc.getPaymentTermDays(),
                    aging.collectionStatus(),
                    aging.daysOverdue(),
                    aging.overdue(),
                    aging.dueSoon()
            ));
        }

        if (Boolean.TRUE.equals(overdueOnly)) {
            rows = rows.stream().filter(CxcDocumentDashboardRow::overdue).toList();
        }
        if (Boolean.TRUE.equals(dueSoonOnly)) {
            rows = rows.stream().filter(CxcDocumentDashboardRow::dueSoon).toList();
        }
        if (Boolean.TRUE.equals(withBalance)) {
            rows = rows.stream()
                    .filter(row -> row.remaining() != null && row.remaining().compareTo(BigDecimal.ZERO) > 0)
                    .toList();
        }
        if (search != null && !search.isBlank()) {
            String needle = search.trim().toLowerCase(Locale.ROOT);
            rows = rows.stream()
                    .filter(row -> contains(row.cxcNumber(), needle)
                            || contains(row.orderNumber(), needle)
                            || contains(row.odpNumber(), needle)
                            || contains(row.clientName(), needle)
                            || contains(row.sourceNumber(), needle)
                            || contains(row.concept(), needle))
                    .toList();
        }

        int from = Math.min(query.offset(), rows.size());
        int to = Math.min(from + query.size(), rows.size());
        return new PageResult<>(rows.subList(from, to), query.page(), query.size(), rows.size());
    }

    private static CxcDocumentConcept parseConcept(String concept) {
        if (concept == null || concept.isBlank()) return null;
        return CxcDocumentConcept.fromValue(concept.trim());
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static AgingBits agingOf(LocalDate dueDate, BigDecimal remaining, LocalDate today) {
        boolean hasBalance = remaining != null && remaining.compareTo(BigDecimal.ZERO) > 0;
        if (!hasBalance || dueDate == null) {
            return new AgingBits(dueDate == null ? "sin_vencimiento" : "al_dia", 0, false, false);
        }
        if (dueDate.isBefore(today)) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(dueDate, today);
            return new AgingBits("vencida", (int) Math.max(0, days), true, false);
        }
        long until = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate);
        boolean dueSoon = until <= 7;
        return new AgingBits(dueSoon ? "por_vencer" : "al_dia", 0, false, dueSoon);
    }

    private record AgingBits(
            String collectionStatus,
            int daysOverdue,
            boolean overdue,
            boolean dueSoon
    ) {
    }

    public record CxcDocumentDashboardRow(
            String cxcDocumentId,
            String cxcNumber,
            String productionOrderId,
            String orderNumber,
            String odpNumber,
            String clientId,
            String clientName,
            String concept,
            String sourceDeliveryId,
            String sourceAdditionId,
            /** ENT-{n} o ADN-{n} según concepto. */
            String sourceNumber,
            BigDecimal total,
            BigDecimal totalApplied,
            BigDecimal remaining,
            String status,
            LocalDateTime issuedAt,
            LocalDate dueDate,
            int paymentTermDays,
            String collectionStatus,
            int daysOverdue,
            boolean overdue,
            boolean dueSoon
    ) {
    }
}
