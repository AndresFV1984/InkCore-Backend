package com.inkcore.application.order.usecase;

import com.inkcore.domain.order.exception.OrderBusinessRuleException;
import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.model.CxcDocumentConcept;
import com.inkcore.domain.order.model.CxcDocumentStatus;
import com.inkcore.domain.order.model.CustomerOrder;
import com.inkcore.domain.order.model.NegotiatedAddition;
import com.inkcore.domain.order.model.NegotiatedAdditionMovementType;
import com.inkcore.domain.order.model.OrderCxcDocument;
import com.inkcore.domain.order.model.OrderDelivery;
import com.inkcore.domain.order.model.OrderPayment;
import com.inkcore.domain.order.model.OrderPaymentApplication;
import com.inkcore.domain.order.model.PaymentApplicationStatus;
import com.inkcore.domain.order.ports.out.OrderCxcDocumentRepositoryPort;
import com.inkcore.domain.order.ports.out.OrderPaymentApplicationRepositoryPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Creación de CxC por entrega/ADN y aplicación de LIQ a CxC / saldo a favor.
 * En producto CxC = factura de cartera.
 */
@Service
public class OrderCxcDocumentService {

    private final OrderCxcDocumentRepositoryPort cxcDocumentRepository;
    private final OrderPaymentApplicationRepositoryPort applicationRepository;

    public OrderCxcDocumentService(
            OrderCxcDocumentRepositoryPort cxcDocumentRepository,
            OrderPaymentApplicationRepositoryPort applicationRepository
    ) {
        this.cxcDocumentRepository = cxcDocumentRepository;
        this.applicationRepository = applicationRepository;
    }

    public OrderCxcDocument createFromDelivery(
            OrderDelivery delivery,
            AccountsReceivable arSummary,
            CustomerOrder customerOrder,
            LocalDateTime now
    ) {
        if (delivery.getTotalValue() == null || delivery.getTotalValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessRuleException("No se puede abrir CxC con total de entrega <= 0");
        }
        var existing = cxcDocumentRepository.findActiveByDeliveryId(
                delivery.getCompanyId(), delivery.getOrderDeliveryId());
        if (existing.isPresent()) {
            return existing.get();
        }

        long prior = cxcDocumentRepository.countByProductionOrderId(
                delivery.getCompanyId(), delivery.getProductionOrderId());
        String cxcNumber;
        if (prior == 0 && arSummary != null && arSummary.getCxcNumber() != null
                && !arSummary.getCxcNumber().isBlank()) {
            cxcNumber = arSummary.getCxcNumber().trim();
        } else {
            cxcNumber = cxcDocumentRepository.allocateNextCxcNumber(delivery.getCompanyId());
        }

        int termDays = arSummary == null ? 0 : Math.max(0, arSummary.getPaymentTermDays());
        LocalDate dueDate = null;
        if (termDays > 0 && delivery.getDeliveredAt() != null) {
            dueDate = delivery.getDeliveredAt().toLocalDate().plusDays(termDays);
        } else if (prior == 0 && arSummary != null) {
            dueDate = arSummary.getDueDate();
        }

        OrderCxcDocument doc = new OrderCxcDocument();
        doc.setCompanyId(delivery.getCompanyId());
        doc.setCxcNumber(cxcNumber);
        doc.setProductionOrderId(delivery.getProductionOrderId());
        doc.setCustomerOrderId(customerOrder == null ? null : customerOrder.getCustomerOrderId());
        doc.setClientId(delivery.getClientId());
        doc.setConcept(CxcDocumentConcept.ENTREGA);
        doc.setSourceDeliveryId(delivery.getOrderDeliveryId());
        doc.setTotal(delivery.getTotalValue().setScale(2, RoundingMode.HALF_UP));
        doc.setStatus(CxcDocumentStatus.PENDIENTE);
        doc.setIssuedAt(delivery.getDeliveredAt() == null ? now : delivery.getDeliveredAt());
        doc.setDueDate(dueDate);
        doc.setPaymentTermDays(termDays);
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);
        return cxcDocumentRepository.save(doc);
    }

    /** Factura ADN vigente → CxC concepto extra (1 ADN → 1 CxC). */
    public OrderCxcDocument createFromAddition(
            NegotiatedAddition addition,
            AccountsReceivable arSummary,
            CustomerOrder customerOrder,
            LocalDateTime now
    ) {
        if (addition.getMovementType() != NegotiatedAdditionMovementType.REGISTRO) {
            throw new OrderBusinessRuleException("Solo se puede abrir CxC desde un ADN vigente");
        }
        if (addition.getVoidedAt() != null) {
            throw new OrderBusinessRuleException("El ADN está anulado");
        }
        if (addition.getAmount() == null || addition.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessRuleException("No se puede abrir CxC con monto de ADN <= 0");
        }
        var existing = cxcDocumentRepository.findActiveByAdditionId(
                addition.getCompanyId(), addition.getOrderNegotiatedAdditionId());
        if (existing.isPresent()) {
            return existing.get();
        }

        int termDays = arSummary == null ? 0 : Math.max(0, arSummary.getPaymentTermDays());
        LocalDate dueDate = null;
        if (termDays > 0) {
            dueDate = now.toLocalDate().plusDays(termDays);
        }

        OrderCxcDocument doc = new OrderCxcDocument();
        doc.setCompanyId(addition.getCompanyId());
        doc.setCxcNumber(cxcDocumentRepository.allocateNextCxcNumber(addition.getCompanyId()));
        doc.setProductionOrderId(addition.getProductionOrderId());
        doc.setCustomerOrderId(customerOrder == null ? null : customerOrder.getCustomerOrderId());
        doc.setClientId(addition.getClientId());
        doc.setConcept(CxcDocumentConcept.EXTRA);
        doc.setSourceAdditionId(addition.getOrderNegotiatedAdditionId());
        doc.setTotal(addition.getAmount().setScale(2, RoundingMode.HALF_UP));
        doc.setStatus(CxcDocumentStatus.PENDIENTE);
        doc.setIssuedAt(now);
        doc.setDueDate(dueDate);
        doc.setPaymentTermDays(termDays);
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);
        return cxcDocumentRepository.save(doc);
    }

    /**
     * Alinea el total de la CxC EXTRA con el monto vigente del ADN.
     * Si ya hay abonos, no permite bajar el total por debajo de lo aplicado.
     * Si no existe CxC activa (ADN legacy), no crea una nueva.
     */
    public OrderCxcDocument syncTotalFromAddition(NegotiatedAddition addition, LocalDateTime now) {
        if (addition == null || addition.getOrderNegotiatedAdditionId() == null) {
            return null;
        }
        var existing = cxcDocumentRepository.findActiveByAdditionId(
                addition.getCompanyId(), addition.getOrderNegotiatedAdditionId());
        if (existing.isEmpty()) {
            return null;
        }
        OrderCxcDocument doc = existing.get();
        BigDecimal newTotal = addition.getAmount() == null
                ? BigDecimal.ZERO
                : addition.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (newTotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessRuleException("No se puede sincronizar CxC con monto de ADN <= 0");
        }
        BigDecimal applied = cxcDocumentRepository.sumApplied(
                doc.getCompanyId(), doc.getCxcDocumentId());
        if (applied.compareTo(BigDecimal.ZERO) > 0 && newTotal.compareTo(applied) < 0) {
            throw new OrderBusinessRuleException(
                    "No se puede bajar el monto del ADN por debajo de lo ya abonado en CxC "
                            + doc.getCxcNumber() + " (" + applied.toPlainString() + ")"
            );
        }
        if (newTotal.compareTo(doc.getTotal()) == 0) {
            return doc;
        }
        doc.setTotal(newTotal);
        refreshCxcStatus(doc, applied, now);
        return doc;
    }

    /**
     * Aplica el pago a la CxC indicada (hasta su saldo) y el excedente a favor del pedido.
     * Si no hay CxC abiertas del concepto, todo va a favor (adelanto).
     */
    public List<OrderPaymentApplication> applyPayment(
            OrderPayment payment,
            String cxcDocumentId,
            String customerOrderId,
            LocalDateTime now
    ) {
        CxcDocumentConcept concept = payment.getPaymentConcept() == null
                ? CxcDocumentConcept.ENTREGA
                : payment.getPaymentConcept();
        List<OrderCxcDocument> open = cxcDocumentRepository.findOpenByProductionOrderAndConcept(
                payment.getCompanyId(), payment.getProductionOrderId(), concept);

        List<OrderPaymentApplication> apps = new ArrayList<>();
        BigDecimal left = payment.getAmount().setScale(2, RoundingMode.HALF_UP);

        if (!open.isEmpty()) {
            if (cxcDocumentId == null || cxcDocumentId.isBlank()) {
                throw new OrderBusinessRuleException(
                        "cxcDocumentId es obligatorio: el pedido tiene CxC con saldo del concepto "
                                + concept.getDbValue()
                );
            }
            OrderCxcDocument target = cxcDocumentRepository
                    .findByIdForUpdate(payment.getCompanyId(), cxcDocumentId.trim())
                    .orElseThrow(() -> new OrderBusinessRuleException("CxC no encontrada"));
            if (!payment.getProductionOrderId().equals(target.getProductionOrderId())) {
                throw new OrderBusinessRuleException("La CxC no pertenece a esta OP");
            }
            if (target.getConcept() != concept) {
                throw new OrderBusinessRuleException(
                        "La CxC es de concepto " + target.getConcept().getDbValue()
                                + "; el pago es " + concept.getDbValue()
                );
            }
            if (target.getStatus() == CxcDocumentStatus.ANULADA
                    || target.getStatus() == CxcDocumentStatus.PAGADA) {
                throw new OrderBusinessRuleException("La CxC no admite abonos (estado "
                        + target.getStatus().getDbValue() + ")");
            }

            BigDecimal applied = cxcDocumentRepository.sumApplied(
                    payment.getCompanyId(), target.getCxcDocumentId());
            BigDecimal remaining = target.getTotal().subtract(applied).setScale(2, RoundingMode.HALF_UP);
            if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                remaining = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            BigDecimal toCxc = left.min(remaining);
            if (toCxc.compareTo(BigDecimal.ZERO) > 0) {
                apps.add(buildApp(payment, customerOrderId, target.getCxcDocumentId(), toCxc, null, now));
                left = left.subtract(toCxc);
                refreshCxcStatus(target, applied.add(toCxc), now);
            }
        }

        if (left.compareTo(BigDecimal.ZERO) > 0) {
            apps.add(buildApp(payment, customerOrderId, null, left, null, now));
        }

        return applicationRepository.saveAll(apps);
    }

    /**
     * Consume saldo a favor del pedido hacia una CxC abierta (sin nuevo LIQ).
     * Las apps de consumo reutilizan el payment_id del favor de origen.
     */
    public List<OrderPaymentApplication> applyFavorToCxc(
            String companyId,
            String productionOrderId,
            String cxcDocumentId,
            BigDecimal amount,
            String customerOrderId,
            LocalDateTime now
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessRuleException("amount debe ser mayor que 0");
        }
        BigDecimal requested = amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal favor = applicationRepository.sumActiveFavor(companyId, productionOrderId);
        if (favor.compareTo(requested) < 0) {
            throw new OrderBusinessRuleException(
                    "Saldo a favor insuficiente (disponible " + favor.toPlainString() + ")"
            );
        }

        OrderCxcDocument target = cxcDocumentRepository
                .findByIdForUpdate(companyId, cxcDocumentId.trim())
                .orElseThrow(() -> new OrderBusinessRuleException("CxC no encontrada"));
        if (!productionOrderId.equals(target.getProductionOrderId())) {
            throw new OrderBusinessRuleException("La CxC no pertenece a esta OP");
        }
        if (target.getStatus() == CxcDocumentStatus.ANULADA
                || target.getStatus() == CxcDocumentStatus.PAGADA) {
            throw new OrderBusinessRuleException("La CxC no admite aplicación de favor (estado "
                    + target.getStatus().getDbValue() + ")");
        }

        BigDecimal alreadyApplied = cxcDocumentRepository.sumApplied(companyId, target.getCxcDocumentId());
        BigDecimal remaining = target.getTotal().subtract(alreadyApplied).setScale(2, RoundingMode.HALF_UP);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new OrderBusinessRuleException("La CxC no tiene saldo pendiente");
        }
        BigDecimal toApply = requested.min(remaining);

        List<OrderPaymentApplication> favorApps =
                applicationRepository.findActiveFavorByProductionOrderId(companyId, productionOrderId);
        Map<String, BigDecimal> consumedByOrigin = consumedByOrigin(companyId, productionOrderId);

        List<OrderPaymentApplication> apps = new ArrayList<>();
        BigDecimal left = toApply;
        for (OrderPaymentApplication favorApp : favorApps) {
            if (left.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal alreadyConsumed = consumedByOrigin.getOrDefault(
                    favorApp.getApplicationId(), BigDecimal.ZERO);
            BigDecimal available = favorApp.getAmount().subtract(alreadyConsumed)
                    .setScale(2, RoundingMode.HALF_UP);
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal slice = left.min(available);
            OrderPaymentApplication consume = new OrderPaymentApplication();
            consume.setCompanyId(companyId);
            consume.setPaymentId(favorApp.getPaymentId());
            consume.setProductionOrderId(productionOrderId);
            consume.setCustomerOrderId(customerOrderId);
            consume.setCxcDocumentId(target.getCxcDocumentId());
            consume.setAmount(slice);
            consume.setOriginApplicationId(favorApp.getApplicationId());
            consume.setStatus(PaymentApplicationStatus.ACTIVO);
            consume.setAppliedAt(now);
            consume.setCreatedAt(now);
            apps.add(consume);
            left = left.subtract(slice);
        }
        if (left.compareTo(BigDecimal.ZERO) > 0) {
            throw new OrderBusinessRuleException("No se pudo desglosar el saldo a favor");
        }

        List<OrderPaymentApplication> saved = applicationRepository.saveAll(apps);
        refreshCxcStatus(target, alreadyApplied.add(toApply), now);
        return saved;
    }

    /** Anula la CxC de una entrega si no tiene aplicaciones activas. */
    public OrderCxcDocument annulFromDelivery(OrderDelivery delivery, LocalDateTime now) {
        var existing = cxcDocumentRepository.findActiveByDeliveryId(
                delivery.getCompanyId(), delivery.getOrderDeliveryId());
        if (existing.isEmpty()) {
            return null;
        }
        OrderCxcDocument doc = existing.get();
        BigDecimal applied = cxcDocumentRepository.sumApplied(
                doc.getCompanyId(), doc.getCxcDocumentId());
        if (applied.compareTo(BigDecimal.ZERO) > 0) {
            throw new OrderBusinessRuleException(
                    "No se puede anular la entrega: la CxC " + doc.getCxcNumber()
                            + " ya tiene abonos aplicados"
            );
        }
        doc.setStatus(CxcDocumentStatus.ANULADA);
        doc.setUpdatedAt(now);
        return cxcDocumentRepository.update(doc);
    }

    /** Anula la CxC de un ADN si no tiene aplicaciones activas. */
    public OrderCxcDocument annulFromAddition(NegotiatedAddition addition, LocalDateTime now) {
        var existing = cxcDocumentRepository.findActiveByAdditionId(
                addition.getCompanyId(), addition.getOrderNegotiatedAdditionId());
        if (existing.isEmpty()) {
            return null;
        }
        OrderCxcDocument doc = existing.get();
        BigDecimal applied = cxcDocumentRepository.sumApplied(
                doc.getCompanyId(), doc.getCxcDocumentId());
        if (applied.compareTo(BigDecimal.ZERO) > 0) {
            throw new OrderBusinessRuleException(
                    "No se puede anular el adicional: la CxC " + doc.getCxcNumber()
                            + " ya tiene abonos aplicados"
            );
        }
        doc.setStatus(CxcDocumentStatus.ANULADA);
        doc.setUpdatedAt(now);
        return cxcDocumentRepository.update(doc);
    }

    /** Tras reversar un LIQ: marca apps del pago y recalcula estado de CxC afectadas. */
    public void reverseApplicationsForPayment(String companyId, String paymentId, LocalDateTime now) {
        List<OrderPaymentApplication> apps = applicationRepository.findByPaymentId(companyId, paymentId);
        List<String> cxcIds = apps.stream()
                .filter(a -> a.getStatus() == PaymentApplicationStatus.ACTIVO)
                .map(OrderPaymentApplication::getCxcDocumentId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        applicationRepository.reverseAllForPayment(companyId, paymentId);
        for (String cxcId : cxcIds) {
            cxcDocumentRepository.findById(companyId, cxcId).ifPresent(doc -> {
                if (doc.getStatus() != CxcDocumentStatus.ANULADA) {
                    refreshCxcStatus(doc, null, now);
                }
            });
        }
    }

    public List<OpenCxcView> listOpenWithRemaining(
            String companyId,
            String productionOrderId,
            CxcDocumentConcept conceptFilter
    ) {
        return listWithRemaining(companyId, productionOrderId, conceptFilter, true);
    }

    /**
     * @param openOnly si true, solo CxC con saldo &gt; 0; si false, todas excepto anuladas.
     */
    public List<OpenCxcView> listWithRemaining(
            String companyId,
            String productionOrderId,
            CxcDocumentConcept conceptFilter,
            boolean openOnly
    ) {
        List<OrderCxcDocument> docs = cxcDocumentRepository.findByProductionOrderId(
                companyId, productionOrderId);
        List<OpenCxcView> result = new ArrayList<>();
        for (OrderCxcDocument doc : docs) {
            if (doc.getStatus() == CxcDocumentStatus.ANULADA) {
                continue;
            }
            if (openOnly && doc.getStatus() == CxcDocumentStatus.PAGADA) {
                continue;
            }
            if (conceptFilter != null && doc.getConcept() != conceptFilter) {
                continue;
            }
            BigDecimal applied = cxcDocumentRepository.sumApplied(companyId, doc.getCxcDocumentId());
            BigDecimal remaining = doc.getTotal().subtract(applied).setScale(2, RoundingMode.HALF_UP);
            if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                remaining = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            if (openOnly && remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            result.add(new OpenCxcView(doc, applied, remaining));
        }
        return result;
    }

    public void refreshCxcStatus(OrderCxcDocument doc, BigDecimal totalApplied, LocalDateTime now) {
        BigDecimal applied = totalApplied == null
                ? cxcDocumentRepository.sumApplied(doc.getCompanyId(), doc.getCxcDocumentId())
                : totalApplied;
        if (applied.compareTo(BigDecimal.ZERO) <= 0) {
            doc.setStatus(CxcDocumentStatus.PENDIENTE);
        } else if (applied.compareTo(doc.getTotal()) >= 0) {
            doc.setStatus(CxcDocumentStatus.PAGADA);
        } else {
            doc.setStatus(CxcDocumentStatus.PARCIAL);
        }
        doc.setUpdatedAt(now);
        cxcDocumentRepository.update(doc);
    }

    private Map<String, BigDecimal> consumedByOrigin(String companyId, String productionOrderId) {
        // Consumos = apps con origin_application_id hacia CxC; sumamos por origen.
        List<OrderPaymentApplication> favorApps =
                applicationRepository.findActiveFavorByProductionOrderId(companyId, productionOrderId);
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (OrderPaymentApplication favor : favorApps) {
            BigDecimal consumed = BigDecimal.ZERO;
            for (OrderPaymentApplication related :
                    applicationRepository.findByPaymentId(companyId, favor.getPaymentId())) {
                if (related.getStatus() != PaymentApplicationStatus.ACTIVO) {
                    continue;
                }
                if (favor.getApplicationId().equals(related.getOriginApplicationId())) {
                    consumed = consumed.add(related.getAmount());
                }
            }
            map.put(favor.getApplicationId(), consumed.setScale(2, RoundingMode.HALF_UP));
        }
        return map;
    }

    private static OrderPaymentApplication buildApp(
            OrderPayment payment,
            String customerOrderId,
            String cxcDocumentId,
            BigDecimal amount,
            String originApplicationId,
            LocalDateTime now
    ) {
        OrderPaymentApplication app = new OrderPaymentApplication();
        app.setCompanyId(payment.getCompanyId());
        app.setPaymentId(payment.getOrderPaymentId());
        app.setProductionOrderId(payment.getProductionOrderId());
        app.setCustomerOrderId(customerOrderId);
        app.setCxcDocumentId(cxcDocumentId);
        app.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        app.setOriginApplicationId(originApplicationId);
        app.setStatus(PaymentApplicationStatus.ACTIVO);
        app.setAppliedAt(payment.getPaidAt() == null ? now : payment.getPaidAt());
        app.setCreatedAt(now);
        return app;
    }

    public record OpenCxcView(
            OrderCxcDocument document,
            BigDecimal totalApplied,
            BigDecimal remaining
    ) {
    }
}
