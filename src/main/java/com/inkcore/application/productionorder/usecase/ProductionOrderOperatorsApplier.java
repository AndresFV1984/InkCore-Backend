package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.productionorder.exception.ProductionOrderBusinessRuleException;
import com.inkcore.domain.productionorder.model.OperatorAssignment;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStage;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import com.inkcore.domain.user.model.User;
import com.inkcore.domain.user.ports.out.UserRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Semántica de persistencia de responsables:
 * <ul>
 *   <li>{@code operators != null} → replace-all del set de la OP (incluye {@code []} = clear).</li>
 *   <li>{@code operators == null} → no tocar; solo entonces aplica el legacy {@code operatorUserId}
 *       como upsert de la etapa del endpoint (si viene no blank).</li>
 * </ul>
 */
@Component
public class ProductionOrderOperatorsApplier {

    private final UserRepositoryPort userRepository;

    public ProductionOrderOperatorsApplier(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    public void apply(
            ProductionOrder order,
            String companyId,
            List<OperatorAssignmentCommand> operators,
            String legacyOperatorUserId,
            ProductionOrderStage legacyStage
    ) {
        if (operators != null) {
            order.replaceOperators(toAssignments(order, companyId, operators));
            return;
        }
        if (legacyOperatorUserId != null && !legacyOperatorUserId.isBlank()) {
            requireUserInCompany(legacyOperatorUserId.trim(), companyId);
            order.upsertOperator(legacyStage, legacyOperatorUserId.trim());
        }
    }

    private List<OperatorAssignment> toAssignments(
            ProductionOrder order,
            String companyId,
            List<OperatorAssignmentCommand> operators
    ) {
        List<String> errors = new ArrayList<>();
        Set<ProductionOrderStage> seen = new HashSet<>();
        List<OperatorAssignment> result = new ArrayList<>();

        for (int i = 0; i < operators.size(); i++) {
            OperatorAssignmentCommand item = operators.get(i);
            if (item == null) {
                errors.add("operators[" + i + "]: ítem nulo");
                continue;
            }

            ProductionOrderStage stage = null;
            if (item.stage() == null || item.stage().isBlank()) {
                errors.add("operators[" + i + "].stage: obligatorio");
            } else {
                try {
                    stage = ProductionOrderStage.fromValue(item.stage());
                } catch (IllegalArgumentException ex) {
                    errors.add("operators[" + i + "].stage: valor inválido");
                }
            }

            String userId = null;
            if (item.userId() == null || item.userId().isBlank()) {
                errors.add("operators[" + i + "].userId: obligatorio");
            } else {
                userId = item.userId().trim();
            }

            if (stage != null && !seen.add(stage)) {
                errors.add("operators[" + i + "].stage: duplicado (" + stage.name() + ")");
                continue;
            }
            if (stage == null || userId == null) {
                continue;
            }

            requireUserInCompany(userId, companyId);
            result.add(OperatorAssignment.of(
                    companyId,
                    order.getProductionOrderId(),
                    stage,
                    userId,
                    item.roleCode()
            ));
        }

        if (!errors.isEmpty()) {
            throw new ProductionOrderBusinessRuleException("Regla de negocio incumplida", errors);
        }
        return result;
    }

    private void requireUserInCompany(String userId, String companyId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "USER_NOT_FOUND", "Usuario no encontrado"));
        if (!companyId.equals(user.getCompanyId())) {
            throw new ResourceNotFoundException("USER_NOT_FOUND", "Usuario no encontrado");
        }
    }
}
