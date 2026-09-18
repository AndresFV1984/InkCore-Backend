package com.inkcore.application.order.usecase;

import com.inkcore.application.order.OrderSupport;
import com.inkcore.domain.client.model.Client;
import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.order.exception.InsufficientAvailabilityException;
import com.inkcore.domain.order.exception.OrderBusinessRuleException;
import com.inkcore.domain.order.model.AccountsReceivable;
import com.inkcore.domain.order.model.DeliveryMovementType;
import com.inkcore.domain.order.model.DeliveryType;
import com.inkcore.domain.order.model.OrderDelivery;
import com.inkcore.domain.order.ports.out.AccountsReceivableRepositoryPort;
import com.inkcore.domain.order.ports.out.OrderDeliveryRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
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

    private static final Pattern AVAILABLE_PATTERN = Pattern.compile(
            "No se puede entregar (\\d+) unidades: solo hay (\\d+) disponibles para la OP ([^\\r\\n]+)",
            Pattern.CASE_INSENSITIVE
    );

    private final OrderSupport support;
    private final OrderDeliveryRepositoryPort deliveryRepository;
    private final AccountsReceivableRepositoryPort accountsReceivableRepository;
    private final ClientRepositoryPort clientRepository;

    public CreateOrderDeliveryUseCase(
            OrderSupport support,
            OrderDeliveryRepositoryPort deliveryRepository,
            AccountsReceivableRepositoryPort accountsReceivableRepository,
            ClientRepositoryPort clientRepository
    ) {
        this.support = support;
        this.deliveryRepository = deliveryRepository;
        this.accountsReceivableRepository = accountsReceivableRepository;
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

        Client client = clientRepository.findById(order.getClientId())
                .filter(found -> companyId.equals(found.getCompanyId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CLIENT_NOT_FOUND",
                        "Cliente de la orden no encontrado"
                ));

        BigDecimal unitPrice = command.unitPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalValue = unitPrice
                .multiply(BigDecimal.valueOf(command.quantityDelivered()))
                .setScale(2, RoundingMode.HALF_UP);

        OrderDelivery delivery = new OrderDelivery();
        delivery.setCompanyId(companyId);
        delivery.setDeliveryNumber(support.nextDeliveryNumber(companyId));
        delivery.setProductionOrderId(order.getProductionOrderId());
        delivery.setClientId(order.getClientId());
        delivery.setSellerId(blankToNull(command.sellerId()));
        delivery.setMovementType(DeliveryMovementType.ENTREGA);
        delivery.setDeliveryType(deliveryType);
        delivery.setQuantityDelivered(command.quantityDelivered());
        delivery.setUnitPrice(unitPrice);
        delivery.setTotalValue(totalValue);
        delivery.setWorkNameSnapshot(order.getWorkName());
        delivery.setClientNameSnapshot(client.getName());
        delivery.setDeliveredAt(command.deliveredAt() == null ? now : command.deliveredAt());
        delivery.setDeliveredBy(userId);
        delivery.setNotes(command.notes());
        delivery.setCreatedAt(now);

        OrderDelivery saved;
        try {
            saved = deliveryRepository.save(delivery);
        } catch (DataIntegrityViolationException ex) {
            throw translateAvailabilityConflict(ex);
        }

        AccountsReceivable summary = accountsReceivableRepository
                .findByProductionOrderId(companyId, order.getProductionOrderId())
                .orElseGet(AccountsReceivable::new);

        return new CreateDeliveryResult(saved, summary);
    }

    private static InsufficientAvailabilityException translateAvailabilityConflict(DataIntegrityViolationException ex) {
        String message = rootMessage(ex);
        Matcher matcher = AVAILABLE_PATTERN.matcher(message == null ? "" : message);
        if (matcher.find()) {
            return new InsufficientAvailabilityException(
                    "No se puede entregar " + matcher.group(1) + " unidades: solo hay "
                            + matcher.group(2) + " disponibles para la OP " + matcher.group(3).trim()
            );
        }
        if (message != null && message.toLowerCase().contains("disponibles")) {
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

    public record CreateDeliveryResult(OrderDelivery delivery, AccountsReceivable accountsReceivable) {
    }
}
