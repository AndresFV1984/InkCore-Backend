package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.productionorder.exception.ProductionOrderBusinessRuleException;
import com.inkcore.domain.productionorder.model.OperatorAssignment;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStage;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import com.inkcore.domain.user.model.User;
import com.inkcore.domain.user.ports.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionOrderOperatorsApplierTest {

    @Mock UserRepositoryPort userRepository;

    private ProductionOrderOperatorsApplier applier;
    private ProductionOrder order;

    @BeforeEach
    void setUp() {
        applier = new ProductionOrderOperatorsApplier(userRepository);
        order = ProductionOrder.reconstitute();
        order.setProductionOrderId("order-1");
        order.setCompanyId("company-1");
        order.replaceOperators(List.of(
                OperatorAssignment.of("company-1", "order-1", ProductionOrderStage.PREPRESS, "existing-user")
        ));
    }

    @Test
    void apply_replaceAllWhenOperatorsPresent() {
        when(userRepository.findById("user-a")).thenReturn(Optional.of(sampleUser("user-a", "company-1")));
        when(userRepository.findById("user-b")).thenReturn(Optional.of(sampleUser("user-b", "company-1")));

        applier.apply(
                order,
                "company-1",
                List.of(
                        new OperatorAssignmentCommand("PREPRESS", "user-a", "OPERARIO"),
                        new OperatorAssignmentCommand("PRINTING", "user-b", null)
                ),
                null,
                ProductionOrderStage.PREPRESS
        );

        assertEquals(2, order.getOperators().size());
        assertEquals("user-a", findUser(ProductionOrderStage.PREPRESS));
        assertEquals("OPERARIO", findRole(ProductionOrderStage.PREPRESS));
        assertEquals("user-b", findUser(ProductionOrderStage.PRINTING));
    }

    @Test
    void apply_clearWhenOperatorsEmptyList() {
        applier.apply(order, "company-1", List.of(), null, ProductionOrderStage.PREPRESS);
        assertTrue(order.getOperators().isEmpty());
    }

    @Test
    void apply_doesNotTouchWhenOperatorsKeyAbsent() {
        applier.apply(order, "company-1", null, null, ProductionOrderStage.CUTTING);
        assertEquals(1, order.getOperators().size());
        assertEquals("existing-user", findUser(ProductionOrderStage.PREPRESS));
    }

    @Test
    void apply_legacyUpsertOnlyWhenOperatorsAbsent() {
        when(userRepository.findById("user-cut")).thenReturn(Optional.of(sampleUser("user-cut", "company-1")));

        applier.apply(order, "company-1", null, "user-cut", ProductionOrderStage.CUTTING);

        assertEquals(2, order.getOperators().size());
        assertEquals("existing-user", findUser(ProductionOrderStage.PREPRESS));
        assertEquals("user-cut", findUser(ProductionOrderStage.CUTTING));
    }

    @Test
    void apply_rejectsUserFromOtherCompany() {
        when(userRepository.findById("user-x")).thenReturn(Optional.of(sampleUser("user-x", "other-company")));

        assertThrows(ResourceNotFoundException.class, () -> applier.apply(
                order,
                "company-1",
                List.of(new OperatorAssignmentCommand("PREPRESS", "user-x", null)),
                null,
                ProductionOrderStage.PREPRESS
        ));
    }

    @Test
    void apply_rejectsInvalidStage() {
        assertThrows(ProductionOrderBusinessRuleException.class, () -> applier.apply(
                order,
                "company-1",
                List.of(new OperatorAssignmentCommand("NOT_A_STAGE", "user-a", null)),
                null,
                ProductionOrderStage.PREPRESS
        ));
    }

    private String findUser(ProductionOrderStage stage) {
        return order.getOperators().stream()
                .filter(o -> o.getStage() == stage)
                .map(OperatorAssignment::getUserId)
                .findFirst()
                .orElse(null);
    }

    private String findRole(ProductionOrderStage stage) {
        return order.getOperators().stream()
                .filter(o -> o.getStage() == stage)
                .map(OperatorAssignment::getRoleCode)
                .findFirst()
                .orElse(null);
    }

    private static User sampleUser(String userId, String companyId) {
        UUID roleId = UUID.fromString("b1ffbc99-9c0b-4ef8-bb6d-6bb9bd380a22");
        return User.reconstitute(
                userId, companyId, "1", "CC", "Op", userId + "@test.com", "",
                "Antioquia", "Medellin", "", "hash", LocalDate.of(2026, 1, 1),
                true, 1L, List.of(roleId), List.of("Operario"), List.of("OPERARIO"),
                List.of(), false, null, null, 0, null, null
        );
    }
}
