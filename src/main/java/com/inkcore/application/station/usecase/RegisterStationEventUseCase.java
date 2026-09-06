package com.inkcore.application.station.usecase;

import com.inkcore.application.station.StationIntervalService;
import com.inkcore.application.station.StationOrderProgressService;
import com.inkcore.application.station.StationProgressService;
import com.inkcore.application.station.StationSupport;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.station.exception.StationBusinessRuleException;
import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.domain.station.model.StationOperationEvent;
import com.inkcore.domain.station.model.StationPhase;
import com.inkcore.domain.station.ports.out.StationOperationEventRepositoryPort;
import com.inkcore.domain.station.service.StationProcessKeyResolver;
import com.inkcore.domain.station.service.StationValidationService;
import com.inkcore.domain.user.ports.out.UserRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RegisterStationEventUseCase {

    private final StationSupport support;
    private final StationOperationEventRepositoryPort eventRepository;
    private final StationIntervalService intervalService;
    private final StationProgressService progressService;
    private final StationOrderProgressService orderProgressService;
    private final StationValidationService validationService;
    private final UserRepositoryPort userRepository;

    public RegisterStationEventUseCase(
            StationSupport support,
            StationOperationEventRepositoryPort eventRepository,
            StationIntervalService intervalService,
            StationProgressService progressService,
            StationOrderProgressService orderProgressService,
            UserRepositoryPort userRepository
    ) {
        this.support = support;
        this.eventRepository = eventRepository;
        this.intervalService = intervalService;
        this.progressService = progressService;
        this.orderProgressService = orderProgressService;
        this.validationService = new StationValidationService();
        this.userRepository = userRepository;
    }

    @Transactional
    public StationOperationEvent execute(RegisterStationEventCommand command, Authentication authentication) {
        String companyId = support.companyId(authentication);
        String actorUserId = support.userId(authentication);
        LocalDateTime now = support.now();
        boolean shiftEvent = Boolean.TRUE.equals(command.shiftEvent())
                || StationProcessKeyResolver.JORNADA_KEY.equalsIgnoreCase(command.processKey());

        ProductionOrder order = null;
        if (!shiftEvent) {
            if (command.productionOrderId() == null || command.productionOrderId().isBlank()) {
                throw new StationBusinessRuleException("productionOrderId es obligatorio");
            }
            order = support.requireOrder(command.productionOrderId(), companyId);
        }

        StationPhase phase = shiftEvent
                ? StationPhase.JORNADA
                : StationPhase.fromApiValue(command.phase());

        StationProcessKeyResolver.ResolvedProcessKey resolved = shiftEvent
                ? new StationProcessKeyResolver.ResolvedProcessKey(
                StationProcessKeyResolver.JORNADA_KEY,
                StationPhase.JORNADA.getApiValue(),
                null, null, null, 0)
                : StationProcessKeyResolver.resolve(order, command.phase(), command.processKey());

        String operatorUserId = command.userId() == null || command.userId().isBlank()
                ? actorUserId
                : command.userId().trim();

        List<StationOperationEvent> existingEvents = shiftEvent || order == null
                ? List.of()
                : eventRepository.findAllByProductionOrderId(companyId, order.getProductionOrderId());

        LocalDateTime occurredAt = command.occurredAt() == null ? now : command.occurredAt();
        validationService.validateOccurredAt(occurredAt, now);
        validationService.validateEventRegistration(
                order,
                operatorUserId,
                phase,
                resolved,
                command.eventType(),
                command.units(),
                command.pauseReason(),
                command.note(),
                shiftEvent,
                existingEvents
        );

        String actorName = userRepository.findById(actorUserId)
                .map(user -> user.getName())
                .orElse(actorUserId);

        StationOperationEvent event = new StationOperationEvent();
        event.setCompanyId(companyId);
        event.setProductionOrderId(shiftEvent ? null : order.getProductionOrderId());
        event.setClientId(shiftEvent ? null : order.getClientId());
        event.setUserId(operatorUserId);
        event.setActorUserId(actorUserId);
        event.setActorName(actorName);
        event.setWorkName(command.workName() != null ? command.workName()
                : (order == null ? null : order.getWorkName()));
        event.setPhase(resolved.phase());
        event.setProcessKey(resolved.processKey());
        event.setCatalogItemKind(resolved.catalogItemKind());
        event.setCatalogItemId(resolved.catalogItemId());
        event.setCatalogItemLabel(resolved.catalogItemLabel());
        event.setEventType(command.eventType());
        event.setOccurredAt(occurredAt);
        event.setUnits(command.units());
        event.setPauseReason(command.pauseReason());
        event.setNote(command.note());
        event.setProductionStatusSnapshot(command.productionStatus() != null
                ? command.productionStatus()
                : (order == null ? null : order.getStatus()));
        event.setOrderStatusSnapshot(order == null ? null : order.getStatus());
        event.setShiftEvent(shiftEvent);
        event.setCreatedAt(now);

        StationOperationEvent saved = eventRepository.save(event);
        intervalService.applyEvent(saved, resolved);

        if (!shiftEvent && order != null) {
            List<StationOperationEvent> updatedEvents =
                    eventRepository.findAllByProductionOrderId(companyId, order.getProductionOrderId());
            progressService.updateFromEvent(saved, resolved, updatedEvents, now);
            if (command.eventType() == StationEventType.AVANCE_UNIDADES) {
                orderProgressService.recalculate(order, updatedEvents, now);
            }
        }

        return saved;
    }
}
