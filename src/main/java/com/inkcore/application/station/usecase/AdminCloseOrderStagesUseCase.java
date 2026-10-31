package com.inkcore.application.station.usecase;

import com.inkcore.application.station.StationIntervalService;
import com.inkcore.application.station.StationOrderCompletionService;
import com.inkcore.application.station.StationOrderProgressService;
import com.inkcore.application.station.StationSupport;
import com.inkcore.domain.productionorder.model.OutsourcedFulfillmentStatus;
import com.inkcore.domain.productionorder.model.OutsourcedStage;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStage;
import com.inkcore.domain.productionorder.model.ProductionOrderStatus;
import com.inkcore.domain.station.exception.StationBusinessRuleException;
import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.model.StationPhase;
import com.inkcore.domain.station.model.StationProcessStatus;
import com.inkcore.domain.station.ports.out.StationOperationEventRepositoryPort;
import com.inkcore.domain.station.ports.out.StationOperationIntervalRepositoryPort;
import com.inkcore.domain.station.service.StationValidationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final StationOrderProgressService orderProgressService;

    public AdminCloseOrderStagesUseCase(
            StationSupport support,
            ListStationOrderProcessesUseCase processesUseCase,
            RegisterStationEventUseCase registerEventUseCase,
            StationOperationEventRepositoryPort eventRepository,
            StationOperationIntervalRepositoryPort intervalRepository,
            StationIntervalService intervalService,
            StationOrderCompletionService orderCompletionService,
            StationOrderProgressService orderProgressService
    ) {
        this.support = support;
        this.processesUseCase = processesUseCase;
        this.registerEventUseCase = registerEventUseCase;
        this.eventRepository = eventRepository;
        this.intervalRepository = intervalRepository;
        this.intervalService = intervalService;
        this.orderCompletionService = orderCompletionService;
        this.orderProgressService = orderProgressService;
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
        String actorUserId = support.userId(authentication);
        LocalDateTime now = support.now();
        List<ListStationOrderProcessesUseCase.StationProcessRow> rows =
                processesUseCase.executeInternal(productionOrderId, companyId);

        List<StationOperationEvent> existingEvents =
                eventRepository.findAllByProductionOrderId(companyId, productionOrderId);

        List<ClosedStage> closed = new ArrayList<>();
        Set<ProductionOrderStage> outsourcedClosedStages = new HashSet<>();
        for (ListStationOrderProcessesUseCase.StationProcessRow row : rows) {
            boolean outsourced = isOutsourcedEnabledForRow(order, row);
            boolean openWork = hasOpenWork(companyId, productionOrderId, row, existingEvents);

            if (outsourced) {
                // Tercerizada: no inventa avance de planta; el cierre es fulfillment COMPLETED.
                if (isOutsourcedFulfillmentCompleteForRow(order, row) && !openWork) {
                    continue;
                }
                if (openWork) {
                    sealOpenStage(order, row, observation, authentication);
                    existingEvents = eventRepository.findAllByProductionOrderId(companyId, productionOrderId);
                }
                ProductionOrderStage stage = resolveOrderStage(row);
                if (stage != null) {
                    outsourcedClosedStages.add(stage);
                }
                closed.add(new ClosedStage(row.phase(), row.processKey(), row.catalogItemLabel()));
                continue;
            }

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

        // Marca todas las etapas tercerizadas habilitadas incompletas (también las que no
        // aparecen en filas de estación, p. ej. terminados solo con proveedor).
        ProductionOrder latest = support.requireOrder(productionOrderId, companyId);
        List<ClosedStage> outsourcedOnlyClosed = markEnabledOutsourcedStagesCompleted(
                latest,
                actorUserId,
                now,
                outsourcedClosedStages
        );
        if (!outsourcedOnlyClosed.isEmpty() || !outsourcedClosedStages.isEmpty()) {
            latest = support.productionOrderRepository().save(latest);
            for (ClosedStage extra : outsourcedOnlyClosed) {
                boolean already = closed.stream().anyMatch(item ->
                        item.phase().equals(extra.phase()) && item.processKey().equals(extra.processKey()));
                if (!already) {
                    closed.add(extra);
                }
            }
        }

        // Tercerización COMPLETED sin AVANCE: recalcular cantidadDisponible del listado OP.
        orderProgressService.recalculateFromPersistedEvents(latest, now);

        // Tras completar cupos/entregas (y tercerización), la OP queda Finalizada (COMPLETED).
        boolean completed = orderCompletionService.completeIfAllStagesProcessed(
                latest,
                actorUserId,
                now
        );
        if (!completed) {
            forceCompletedAfterAdminClose(latest, authentication);
        }

        return new Result(order.getProductionOrderId(), observation, closed);
    }

    /**
     * Cierra el ciclo de proveedor (COMPLETED) en etapas tercerizadas habilitadas.
     * @return etapas que no estaban en filas de estación y se agregaron al resultado
     */
    private List<ClosedStage> markEnabledOutsourcedStagesCompleted(
            ProductionOrder order,
            String actorUserId,
            LocalDateTime now,
            Set<ProductionOrderStage> alreadyCounted
    ) {
        LocalDate today = now.toLocalDate();
        List<ClosedStage> extras = new ArrayList<>();
        if (order.getOutsourcedStages() == null || order.getOutsourcedStages().isEmpty()) {
            return extras;
        }
        for (OutsourcedStage stageRow : order.getOutsourcedStages()) {
            if (stageRow == null || !stageRow.isEnabled() || stageRow.isFulfillmentComplete()) {
                continue;
            }
            if (stageRow.getSentAt() == null) {
                stageRow.setSentAt(today);
            }
            if (stageRow.getReceivedAt() == null) {
                stageRow.setReceivedAt(today);
            }
            if (stageRow.getReceivedByUserId() == null || stageRow.getReceivedByUserId().isBlank()) {
                stageRow.setReceivedByUserId(actorUserId);
            }
            String priorNote = stageRow.getSentDescription();
            String adminNote = NOTE_PREFIX + " (estación)";
            if (priorNote == null || priorNote.isBlank()) {
                stageRow.setSentDescription(adminNote);
            } else if (!priorNote.contains(NOTE_PREFIX)) {
                stageRow.setSentDescription(priorNote + " · " + adminNote);
            }
            stageRow.setFulfillmentStatus(OutsourcedFulfillmentStatus.COMPLETED);

            ProductionOrderStage stage = stageRow.getStage();
            if (stage != null && (alreadyCounted == null || !alreadyCounted.contains(stage))) {
                String phase = stationPhaseApiValue(stage);
                extras.add(new ClosedStage(phase, phase, stageRow.getSupplierName()));
            }
        }
        return extras;
    }

    private static boolean isOutsourcedEnabledForRow(
            ProductionOrder order,
            ListStationOrderProcessesUseCase.StationProcessRow row
    ) {
        ProductionOrderStage stage = resolveOrderStage(row);
        if (stage == null || order.getOutsourcedStages() == null) {
            return false;
        }
        return order.getOutsourcedStages().stream()
                .anyMatch(item -> item != null && item.isEnabled() && item.getStage() == stage);
    }

    private static boolean isOutsourcedFulfillmentCompleteForRow(
            ProductionOrder order,
            ListStationOrderProcessesUseCase.StationProcessRow row
    ) {
        ProductionOrderStage stage = resolveOrderStage(row);
        if (stage == null || order.getOutsourcedStages() == null) {
            return false;
        }
        return order.getOutsourcedStages().stream()
                .anyMatch(item -> item != null && item.getStage() == stage && item.isFulfillmentComplete());
    }

    private static ProductionOrderStage resolveOrderStage(
            ListStationOrderProcessesUseCase.StationProcessRow row
    ) {
        if (row == null || row.phase() == null || row.phase().isBlank()) {
            return null;
        }
        try {
            return StationPhase.fromApiValue(row.phase()).getOrderStage().orElse(null);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String stationPhaseApiValue(ProductionOrderStage stage) {
        for (StationPhase phase : StationPhase.values()) {
            if (phase.getOrderStage().map(s -> s == stage).orElse(false)) {
                return phase.getApiValue();
            }
        }
        return stage.name();
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
                        null
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
