package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.order.exception.OrderBusinessRuleException;
import com.inkcore.domain.order.exception.OrderConflictException;
import com.inkcore.domain.order.model.ArSummary;
import com.inkcore.domain.order.model.DeliveryType;
import com.inkcore.domain.order.model.OrderDelivery;
import com.inkcore.domain.order.ports.out.ArSummaryRepositoryPort;
import com.inkcore.domain.order.ports.out.OrderDeliveryRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CreateOrderDeliveryUseCase {

    public static final String STATUS_ENTREGADO = "ENTREGADO";

    private static final Pattern AVAILABLE_PATTERN = Pattern.compile("solo hay (\\d+) disponibles", Pattern.CASE_INSENSITIVE);

    private final OrderSupport support;
    private final OrderDeliveryRepositoryPort deliveryRepository;
    private final ArSummaryRepositoryPort arSummaryRepository;
    private final ClientRepositoryPort clientRepository;

    public CreateOrderDeliveryUseCase(
            OrderSupport support,
            OrderDeliveryRepositoryPort deliveryRepository,
            ArSummaryRepositoryPort arSummaryRepository,
            ClientRepositoryPort clientRepository
    ) {
        this.support = support;
        this.deliveryRepository = deliveryRepository;
        this.arSummaryRepository = arSummaryRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional
    public CreateDeliveryResult execute(CreateOrderDeliveryCommand command, Authentication authentication) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        LocalDateTime now = support.now();

        if (command.quantityDelivered() == null || command.quantityDelivered() <= 0) {
            throw new OrderBusinessRuleException("quantityDelivered debe ser mayor que 0");
        }
        if (command.unitPrice() == null || command.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new OrderBusinessRuleException("unitPrice debe ser mayor o igual a 0");
        }

        DeliveryType deliveryType = DeliveryType.fromValue(command.deliveryType());
        ProductionOrder order = support.requireActiveOrder(command.productionOrderId(), companyId);

        String clientName = clientRepository.findById(order.getClientId())
                .map(c -> c.getName())
                .orElse(null);

        BigDecimal unitPrice = command.unitPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalValue = unitPrice
                .multiply(BigDecimal.valueOf(command.quantityDelivered()))
                .setScale(2, RoundingMode.HALF_UP);

        OrderDelivery delivery = new OrderDelivery();
        delivery.setCompanyId(companyId);
        delivery.setProductionOrderId(order.getProductionOrderId());
        delivery.setClientId(order.getClientId());
        delivery.setSellerId(blankToNull(command.sellerId()));
        delivery.setDeliveryType(deliveryType);
        delivery.setQuantityDelivered(command.quantityDelivered());
        delivery.setUnitPrice(unitPrice);
        delivery.setTotalValue(totalValue);
        delivery.setAvailableBefore(0);
        delivery.setWorkNameSnapshot(order.getWorkName());
        delivery.setClientNameSnapshot(clientName);
        delivery.setDeliveredAt(command.deliveredAt() == null ? now : command.deliveredAt());
        delivery.setDeliveredBy(userId);
        delivery.setNotes(command.notes());
        delivery.setCreatedAt(now);

        OrderDelivery saved;
        try {
            saved = deliveryRepository.save(delivery);
            saved = deliveryRepository.findById(companyId, saved.getOrderDeliveryId()).orElse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw translateAvailabilityConflict(ex);
        }

        if (deliveryType == DeliveryType.TOTAL) {
            order.setStatus(STATUS_ENTREGADO);
            order.setUpdatedAt(now);
            order.setUpdatedBy(userId);
            support.productionOrderRepository().save(order);
        }

        ArSummary summary = arSummaryRepository
                .findByProductionOrderId(companyId, order.getProductionOrderId())
                .orElseGet(ArSummary::new);

        return new CreateDeliveryResult(saved, summary);
    }

    private static OrderConflictException translateAvailabilityConflict(DataIntegrityViolationException ex) {
        String message = rootMessage(ex);
        Matcher matcher = AVAILABLE_PATTERN.matcher(message == null ? "" : message);
        if (matcher.find()) {
            return new OrderConflictException(
                    "Solo hay " + matcher.group(1) + " unidades disponibles para entregar"
            );
        }
        if (message != null && message.toLowerCase().contains("disponibles")) {
            return new OrderConflictException(message);
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CreateOrderDeliveryCommand(
            String productionOrderId,
            String deliveryType,
            Integer quantityDelivered,
            BigDecimal unitPrice,
            String sellerId,
            LocalDateTime deliveredAt,
            String notes
    ) {
    }

    public record CreateDeliveryResult(OrderDelivery delivery, ArSummary accountsReceivable) {
    }
}
