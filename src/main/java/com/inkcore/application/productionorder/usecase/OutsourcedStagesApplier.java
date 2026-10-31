package com.inkcore.application.productionorder.usecase;

import com.inkcore.application.station.StationOrderProgressService;
import com.inkcore.domain.productionorder.exception.ProductionOrderBusinessRuleException;
import com.inkcore.domain.productionorder.model.OutsourcedFulfillmentStatus;
import com.inkcore.domain.productionorder.model.OutsourcedStage;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Aplica payloads opcionales de tercerización sin romper clientes antiguos.
 * null = no tocar; objeto/lista = upsert o replace-all.
 * Tras mutar fulfillment, sincroniza {@code cantidadDisponible} del listado OP.
 */
@Component
public class OutsourcedStagesApplier {

    private static final Set<ProductionOrderStage> ALLOWED = EnumSet.of(
            ProductionOrderStage.PREPRESS,
            ProductionOrderStage.CUTTING,
            ProductionOrderStage.PRINTING,
            ProductionOrderStage.FINISHED_PRODUCTS,
            ProductionOrderStage.FINISHING_PROCESSES
    );

    private final StationOrderProgressService orderProgressService;

    public OutsourcedStagesApplier(StationOrderProgressService orderProgressService) {
        this.orderProgressService = orderProgressService;
    }

    public void applySingle(ProductionOrder order, OutsourcedStageCommand command) {
        if (command == null) {
            return;
        }
        order.upsertOutsourcedStage(buildStage(order, command));
        syncCantidadDisponible(order);
    }

    /**
     * null = no modifica. Lista (también vacía) = replace-all.
     */
    public void applyReplaceAll(ProductionOrder order, List<OutsourcedStageCommand> commands) {
        if (commands == null) {
            return;
        }
        List<OutsourcedStage> next = new ArrayList<>();
        for (OutsourcedStageCommand command : commands) {
            if (command == null) {
                continue;
            }
            next.add(buildStage(order, command));
        }
        order.replaceOutsourcedStages(next);
        syncCantidadDisponible(order);
    }

    private void syncCantidadDisponible(ProductionOrder order) {
        LocalDateTime now = order.getUpdatedAt() != null ? order.getUpdatedAt() : LocalDateTime.now();
        orderProgressService.recalculateFromPersistedEvents(order, now);
    }

    private OutsourcedStage buildStage(ProductionOrder order, OutsourcedStageCommand command) {
        ProductionOrderStage stage = parseStage(command.stage());
        boolean enabled = command.enabled() == null || command.enabled();
        if (command.quantity() != null && command.quantity() < 0) {
            throw new ProductionOrderBusinessRuleException(
                    "Regla de negocio incumplida",
                    List.of("outsourcedStages.quantity no puede ser negativo")
            );
        }
        OutsourcedFulfillmentStatus status = parseFulfillment(command.fulfillmentStatus());
        status = OutsourcedFulfillmentStatus.resolve(status, command.receivedAt(), command.sentAt());
        if (status == OutsourcedFulfillmentStatus.COMPLETED
                && command.sentAt() == null
                && command.receivedAt() == null) {
            throw new ProductionOrderBusinessRuleException(
                    "Regla de negocio incumplida",
                    List.of("outsourcedStages: COMPLETED requiere sentAt o receivedAt")
            );
        }
        if (command.receivedAt() != null && status == OutsourcedFulfillmentStatus.COMPLETED) {
            status = OutsourcedFulfillmentStatus.COMPLETED;
        }
        return OutsourcedStage.of(
                order.getCompanyId(),
                order.getProductionOrderId(),
                stage,
                enabled,
                nullToZero(command.vendorCost()),
                nullToZero(command.markupPercent()),
                command.sellPrice(),
                command.supplierId(),
                command.supplierName(),
                command.quotedAt(),
                command.quantity(),
                command.costResponsibleUserId(),
                command.costResponsibleUserName(),
                command.quoteValidUntil(),
                command.sentAt(),
                command.sentDescription(),
                command.expectedReturnAt(),
                command.receivedAt(),
                command.receivedByUserId(),
                status
        );
    }

    private static OutsourcedFulfillmentStatus parseFulfillment(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return OutsourcedFulfillmentStatus.fromValue(raw);
        } catch (IllegalArgumentException ex) {
            throw new ProductionOrderBusinessRuleException(
                    "Regla de negocio incumplida",
                    List.of("outsourcedStages.fulfillmentStatus inválido: " + raw
                            + " (use PENDING|DISPATCHED|IN_PROGRESS|COMPLETED)")
            );
        }
    }

    private static ProductionOrderStage parseStage(String raw) {
        ProductionOrderStage stage;
        try {
            stage = ProductionOrderStage.fromValue(raw);
        } catch (IllegalArgumentException ex) {
            throw new ProductionOrderBusinessRuleException(
                    "Regla de negocio incumplida",
                    List.of("outsourcedStages.stage inválido: " + raw)
            );
        }
        if (stage == null || !ALLOWED.contains(stage)) {
            throw new ProductionOrderBusinessRuleException(
                    "Regla de negocio incumplida",
                    List.of("outsourcedStages.stage debe ser PREPRESS|CUTTING|PRINTING|FINISHED_PRODUCTS|FINISHING_PROCESSES")
            );
        }
        return stage;
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
