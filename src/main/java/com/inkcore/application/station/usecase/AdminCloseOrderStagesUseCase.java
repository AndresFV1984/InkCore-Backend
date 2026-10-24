package com.inkcore.application.station.usecase;

import com.inkcore.application.station.StationIntervalService;
import com.inkcore.application.station.StationOrderCompletionService;
import com.inkcore.application.station.StationSupport;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStatus;
import com.inkcore.domain.station.exception.StationBusinessRuleException;
import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.model.StationProcessStatus;
import com.inkcore.domain.station.ports.out.StationOperationEventRepositoryPort;
import com.inkcore.domain.station.ports.out.StationOperationIntervalRepositoryPort;
import com.inkcore.domain.station.service.StationValidationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Cierre administrativo de todas las etapas incompletas de una OP:
 * completa unidades pendientes, registra entrega total del cupo y {@code fin_fase}
 * con observación «Cerrado por administrador».
 */
@Service
public class AdminCloseOrderStagesUseCase {

    public static final String NOTE_PREFIX = "Cerrado por administrador";

    private final StationSupport support;
    private final ListStationOrderProcessesUseCase processesUseCase;
    private final RegisterStationEventUseCase registerEventUseCase;
    private final StationOperationEventRepositoryPort eventRepository;
    private final StationOperationIntervalRepositoryPort intervalRepository;
    private final StationIntervalService intervalService;
    private final StationOrderCompletionService orderCompletionService;

    public AdminCloseOrderStagesUseCase(
            StationSupport support,
            ListStationOrderProcessesUseCase processesUseCase,
            RegisterStationEventUseCase registerEventUseCase,
            StationOperationEventRepositoryPort eventRepository,
            StationOperationIntervalRepositoryPort intervalRepository,
            StationIntervalService intervalService,
            StationOrderCompletionService orderCompletionService
    ) {
        this.support = support;
        this.processesUseCase = processesUseCase;
        this.registerEventUseCase = registerEventUseCase;
        this.eventRepository = eventRepository;
        this.intervalRepository = intervalRepository;
        this.intervalService = intervalService;
        this.orderCompletionService = orderCompletionService;
    }

    @Transactional
    public Result execute(String productionOrderId, String note, Authentication authentication) {
        if (!RegisterStationEventUseCase.isAdministrator(authentication)) {
            throw new AccessDeniedException("Solo el administrador puede cerrar todas las etapas de la OP");
        }
        String trimmedNote = note == null ? "" : note.trim();
        if (trimmedNote.isBlank()) {
            throw new StationBusinessRuleException(
                    "Debe indicar una nota de cierre administrativo"
            );
        }

        String companyId = support.companyId(authentication);
        ProductionOrder order = support.requireOrder(productionOrderId, companyId);
        order = ensureCanonicalPlantStatusForAdminClose(order, authentication);
        StationValidationService validation = new StationValidationService();
        validation.requireExecutionAllowed(order);

        String observation = NOTE_PREFIX + ": " + trimmedNote;
        List<ListStationOrderProcessesUseCase.StationProcessRow> rows =
                processesUseCase.executeInternal(productionOrderId, companyId);

        List<StationOperationEvent> existingEvents =
                eventRepository.findAllByProductionOrderId(companyId, productionOrderId);

        List<ClosedStage> closed = new ArrayList<>();
        for (ListStationOrderProcessesUseCase.StationProcessRow row : rows) {
            boolean openWork = hasOpenWork(companyId, productionOrderId, row, existingEvents);
            if (isFullyClosed(row) && !openWork) {
                continue;
            }
            if (isFullyClosed(row)) {
                // Cupo cumplido pero el reloj sigue abierto: solo sella con fin_fase.
                sealOpenStage(order, row, observation, authentication);
            } else {
                closeStage(order, row, observation, authentication);
            }
            closed.add(new ClosedStage(row.phase(), row.processKey(), row.catalogItemLabel()));
            existingEvents = eventRepository.findAllByProductionOrderId(companyId, productionOrderId);
        }

        // Remanentes (p. ej. intervalos de otro userId) tras los fin_fase.
        existingEvents.stream()
                .filter(event -> event.getEventType() == StationEventType.FIN_FASE)
                .reduce((left, right) ->
                        left.getOccurredAt() != null
                                && right.getOccurredAt() != null
                                && right.getOccurredAt().isAfter(left.getOccurredAt())
                                ? right
                                : left)
                .ifPresent(intervalService::closeAllOpenWorkIntervalsForOrder);

        // Tras completar cupos/entregas, la OP queda Finalizada (COMPLETED).
        ProductionOrder latest = support.requireOrder(productionOrderId, companyId);
        boolean completed = orderCompletionService.completeIfAllStagesProcessed(
                latest,
                support.userId(authentication),
                support.now()
        );
        if (!completed) {
            forceCompletedAfterAdminClose(latest, authentication);
        }

        return new Result(order.getProductionOrderId(), observation, closed);
    }

    /**
     * Normaliza status legacy a {@code IN_PROGRESS_*} y reabre planta no anulada
     * ({@code COMPLETED}, {@code PENDING}, {@code PAUSED}, …) para poder registrar el cierre admin.
     */
    private ProductionOrder ensureCanonicalPlantStatusForAdminClose(
            ProductionOrder order,
            Authentication authentication
    ) {
        if (!order.isState()) {
            throw new StationBusinessRuleException(
                    "La orden está inactiva (archivada); no se pueden registrar avances."
            );
        }
        if (ProductionOrderStatus.isAnuladaAlias(order.getStatus())) {
            throw new StationBusinessRuleException(
                    StationValidationService.EXECUTION_BLOCKED_MESSAGE
                            + " (status="
                            + order.getStatus()
                            + ", state="
                            + order.isState()
                            + ")"
            );
        }
        String canonical = StationValidationService.toCanonicalInProgressWire(order.getStatus());
        if (canonical == null) {
            // Cierre admin: reabrir Finalizada / Pendiente / Pausada a En Proceso.
            canonical = ProductionOrderStatus.IN_PROGRESS.getWireValue();
        }
        if (canonical.equals(order.getStatus())) {
            return order;
        }
        order.setStatus(canonical);
        order.setUpdatedAt(support.now());
        order.setUpdatedBy(support.userId(authentication));
        return support.productionOrderRepository().saveRoot(order);
    }

    private void forceCompletedAfterAdminClose(ProductionOrder order, Authentication authentication) {
        if (order == null || ProductionOrderStatus.isAnuladaAlias(order.getStatus())) {
            return;
        }
        if (ProductionOrderStatus.COMPLETED.getWireValue().equalsIgnoreCase(order.getStatus())) {
            return;
        }
        order.setStatus(ProductionOrderStatus.COMPLETED.getWireValue());
        order.setUpdatedAt(support.now());
        order.setUpdatedBy(support.userId(authentication));
        support.productionOrderRepository().saveRoot(order);
    }

    private void sealOpenStage(
            ProductionOrder order,
            ListStationOrderProcessesUseCase.StationProcessRow row,
            String observation,
            Authentication authentication
    ) {
        String operatorUserId = row.userId();
        List<StationOperationEvent> existing =
                eventRepository.findAllByProductionOrderId(order.getCompanyId(), order.getProductionOrderId());
        if (operatorUserId != null && !operatorUserId.isBlank()
                && StationValidationService.hasOpenPause(
                existing, row.processKey(), operatorUserId, row.catalogItemId())) {
            register(order, row, operatorUserId, StationEventType.REANUDACION, null, null, authentication);
        }
        register(order, row, operatorUserId, StationEventType.FIN_FASE, null, observation, authentication);
    }

    private boolean hasOpenWork(
            String companyId,
            String productionOrderId,
            ListStationOrderProcessesUseCase.StationProcessRow row,
            List<StationOperationEvent> events
    ) {
        String operatorUserId = row.userId();
        if (operatorUserId != null && !operatorUserId.isBlank()
                && StationValidationService.hasOpenPause(
                events, row.processKey(), operatorUserId, row.catalogItemId())) {
            return true;
        }
        return !intervalRepository
                .findOpenByProductionOrderIdAndProcessKey(companyId, productionOrderId, row.processKey())
                .isEmpty();
    }

    private void closeStage(
            ProductionOrder order,
            ListStationOrderProcessesUseCase.StationProcessRow row,
            String observation,
            Authentication authentication
    ) {
        String companyId = order.getCompanyId();
        String productionOrderId = order.getProductionOrderId();
        String operatorUserId = row.userId();

        List<StationOperationEvent> existing =
                eventRepository.findAllByProductionOrderId(companyId, productionOrderId);

        if (operatorUserId != null && !operatorUserId.isBlank()
                && StationValidationService.hasOpenPause(
                existing, row.processKey(), operatorUserId, row.catalogItemId())) {
            register(order, row, operatorUserId, StationEventType.REANUDACION, null, null, authentication);
        }

        int pendingAdvance = Math.max(0, row.totalUnits() - row.completedUnits());
        if (pendingAdvance > 0) {
            register(order, row, operatorUserId, StationEventType.AVANCE_UNIDADES, pendingAdvance, null, authentication);
        }

        // Recalcular entregable tras el avance (misma TX: leer eventos frescos).
        List<StationOperationEvent> afterAdvance =
                eventRepository.findAllByProductionOrderId(companyId, productionOrderId);
        int completed = StationValidationService.sumUnits(
                afterAdvance,
                StationEventType.AVANCE_UNIDADES,
                row.processKey(),
                row.catalogItemId()
        );
        int delivered = StationValidationService.sumDeliveryUnits(
                afterAdvance,
                row.processKey(),
                row.catalogItemId()
        );
        int pendingDelivery = Math.max(0, completed - delivered);
        if (pendingDelivery > 0) {
            register(
                    order,
                    row,
                    operatorUserId,
                    StationEventType.ENTREGA_TOTAL,
                    pendingDelivery,
                    observation,
                    authentication
            );
        }

        register(order, row, operatorUserId, StationEventType.FIN_FASE, null, observation, authentication);
    }

    private void register(
            ProductionOrder order,
            ListStationOrderProcessesUseCase.StationProcessRow row,
            String operatorUserId,
            StationEventType type,
            Integer units,
            String note,
            Authentication authentication
    ) {
        registerEventUseCase.execute(
                new RegisterStationEventCommand(
                        order.getProductionOrderId(),
                        order.getWorkName(),
                        row.phase(),
                        row.processKey(),
                        operatorUserId,
                        units,
                        order.getStatus(),
                        note,
                        null,
                        null,
                        false,
                        type,
                        null,
                        null,
                        null,
                        null,
                        null,
                        true
                ),
                authentication
        );
    }

    private static boolean isFullyClosed(ListStationOrderProcessesUseCase.StationProcessRow row) {
        if (StationProcessStatus.TERMINADO.getDbValue().equalsIgnoreCase(row.status())) {
            return true;
        }
        return row.totalUnits() > 0
                && row.completedUnits() >= row.totalUnits()
                && row.deliveredUnits() >= row.totalUnits();
    }

    public record ClosedStage(String phase, String processKey, String catalogItemLabel) {
    }

    public record Result(
            String productionOrderId,
            String note,
            List<ClosedStage> closedStages
    ) {
    }
}
