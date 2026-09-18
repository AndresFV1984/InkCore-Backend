package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.order.exception.InsufficientAvailabilityException;
import com.inkcore.domain.order.exception.OrderBusinessRuleException;
import com.inkcore.domain.order.exception.OrderConflictException;
import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.model.DeliveryMovementType;
import com.inkcore.domain.order.model.OrderDelivery;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
import com.inkcore.domain.order.ports.out.OrderDeliveryRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class ReverseOrderDeliveryUseCase {

    private final OrderSupport support;
    private final OrderDeliveryRepositoryPort deliveryRepository;
    private final AccountsReceivableRepositoryPort accountsReceivableRepository;

    public ReverseOrderDeliveryUseCase(
            OrderSupport support,
            OrderDeliveryRepositoryPort deliveryRepository,
            AccountsReceivableRepositoryPort accountsReceivableRepository
    ) {
        this.support = support;
        this.deliveryRepository = deliveryRepository;
        this.accountsReceivableRepository = accountsReceivableRepository;
    }

    @Transactional
    public CreateOrderDeliveryUseCase.CreateDeliveryResult execute(
            String productionOrderId,
            String deliveryId,
            String reason,
            Authentication authentication
    ) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        LocalDateTime now = support.now();

        support.requireActiveOrder(productionOrderId, companyId);
        OrderDelivery original = deliveryRepository.findByIdForUpdate(companyId, deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ORDER_DELIVERY_NOT_FOUND",
                        "Entrega no encontrada"
                ));

        if (!productionOrderId.equals(original.getProductionOrderId())) {
            throw new ResourceNotFoundException("ORDER_DELIVERY_NOT_FOUND", "Entrega no encontrada");
        }
        if (original.getMovementType() != DeliveryMovementType.ENTREGA) {
            throw new OrderBusinessRuleException("Solo se pueden anular entregas (no reversiones)");
        }
        if (deliveryRepository.existsReversionFor(companyId, deliveryId)) {
            throw new OrderConflictException("Esta entrega ya fue anulada");
        }

        AccountsReceivable current = accountsReceivableRepository.findByProductionOrderId(companyId, productionOrderId)
                .orElse(null);
        if (current != null) {
            BigDecimal owed = current.getTotalOwed() == null ? BigDecimal.ZERO : current.getTotalOwed();
            BigDecimal paid = current.getTotalPaid() == null ? BigDecimal.ZERO : current.getTotalPaid();
            if (owed.subtract(original.getTotalValue()).compareTo(paid) < 0) {
                throw new OrderBusinessRuleException(
                        "No se puede anular la entrega: el saldo adeudado quedaría por debajo de lo ya abonado"
                );
            }
        }

        OrderDelivery reversion = new OrderDelivery();
        reversion.setCompanyId(companyId);
        reversion.setDeliveryNumber(support.nextDeliveryNumber(companyId));
        reversion.setProductionOrderId(original.getProductionOrderId());
        reversion.setClientId(original.getClientId());
        reversion.setSellerId(original.getSellerId());
        reversion.setMovementType(DeliveryMovementType.REVERSION);
        reversion.setDeliveryType(original.getDeliveryType());
        reversion.setReversedDeliveryId(original.getOrderDeliveryId());
        reversion.setQuantityDelivered(original.getQuantityDelivered());
        reversion.setUnitPrice(original.getUnitPrice());
        reversion.setTotalValue(original.getTotalValue());
        reversion.setWorkNameSnapshot(original.getWorkNameSnapshot());
        reversion.setClientNameSnapshot(original.getClientNameSnapshot());
        reversion.setDeliveredAt(now);
        reversion.setDeliveredBy(userId);
        reversion.setNotes(reason == null || reason.isBlank()
                ? "Entrega anulada"
                : "Anulado por: " + reason.trim());
        reversion.setCreatedAt(now);

        OrderDelivery saved;
        try {
            saved = deliveryRepository.save(reversion);
        } catch (DataIntegrityViolationException ex) {
            throw translateReversalConflict(ex);
        }

        AccountsReceivable summary = accountsReceivableRepository
                .findByProductionOrderId(companyId, productionOrderId)
                .orElseGet(AccountsReceivable::new);
        return new CreateOrderDeliveryUseCase.CreateDeliveryResult(saved, summary);
    }

    private static RuntimeException translateReversalConflict(DataIntegrityViolationException ex) {
        String message = rootMessage(ex);
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);
        if (normalized.contains("ya fue anulada") || normalized.contains("reversed once")) {
            return new OrderConflictException(
                    message == null || message.isBlank() ? "Esta entrega ya fue anulada" : message
            );
        }
        if (normalized.contains("saldo adeudado") || normalized.contains("por debajo de lo ya abonado")) {
            return new OrderBusinessRuleException(message);
        }
        if (normalized.contains("disponibles")) {
            return new InsufficientAvailabilityException(message);
        }
        throw ex;
    }

    private static String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}
