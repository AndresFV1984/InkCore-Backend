package com.inkcore.domain.station.service;

import com.inkcore.domain.productionorder.model.OperatorAssignment;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStage;
import com.inkcore.domain.station.exception.StationBusinessRuleException;
import com.inkcore.domain.station.model.StationEventType;
import com.inkcore.domain.station.model.StationPhase;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StationValidationServiceAdminTest {

    private final StationValidationService validation = new StationValidationService();

    private static ProductionOrder inProgressOrderWithPrepressOperator() {
        ProductionOrder order = ProductionOrder.createNew(
                "co-1",
                "OP-1",
                "cli-1",
                "Trabajo",
                "seller-1",
                LocalDate.of(2026, 10, 20),
                1000,
                null,
                null,
                "admin-1",
                LocalDateTime.of(2026, 10, 20, 10, 0)
        );
        order.setStatus("IN_PROGRESS");
        order.getOperators().add(OperatorAssignment.of(
                order.getCompanyId(),
                order.getProductionOrderId(),
                ProductionOrderStage.PREPRESS,
                "op-1"
        ));
        return order;
    }

    @Test
    void adminCanRegisterWithoutBeingAssigned() {
        ProductionOrder order = inProgressOrderWithPrepressOperator();
        StationProcessKeyResolver.ResolvedProcessKey resolved =
                StationProcessKeyResolver.resolve(order, "preprensa", "preprensa");

        assertDoesNotThrow(() -> validation.validateEventRegistration(
                order,
                "admin-1",
                StationPhase.PREPRENSA,
                resolved,
                StationEventType.FIN_FASE,
                null,
                null,
                "Cerrado por administrador: prueba",
                false,
                List.of(),
                true
        ));
    }

    @Test
    void nonAdminStillRequiresAssignment() {
        ProductionOrder order = inProgressOrderWithPrepressOperator();
        StationProcessKeyResolver.ResolvedProcessKey resolved =
                StationProcessKeyResolver.resolve(order, "preprensa", "preprensa");

        assertThrows(StationBusinessRuleException.class, () -> validation.validateEventRegistration(
                order,
                "other-user",
                StationPhase.PREPRENSA,
                resolved,
                StationEventType.FIN_FASE,
                null,
                null,
                "nota",
                false,
                List.of(),
                false
        ));
    }
}
