package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.paper.model.PaperRemnant;
import com.inkcore.domain.paper.model.PaperRemnantStatus;
import com.inkcore.domain.paper.ports.out.PaperRemnantRepositoryPort;
import com.inkcore.domain.productionorder.model.ProductionOrderStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sincroniza el estado de remanentes generados por una OP cuando cambia el estado de la OP.
 * Regla: estado OP → remanente; {@link ProductionOrderStatus#COMPLETED} → {@link PaperRemnantStatus#AVAILABLE}.
 */
@Component
public class PaperRemnantStatusSync {

    private final PaperRemnantRepositoryPort remnantRepository;

    public PaperRemnantStatusSync(PaperRemnantRepositoryPort remnantRepository) {
        this.remnantRepository = remnantRepository;
    }

    public void syncFromProductionOrder(
            String companyId,
            String productionOrderId,
            ProductionOrderStatus orderStatus,
            LocalDateTime now
    ) {
        if (productionOrderId == null || productionOrderId.isBlank() || orderStatus == null) {
            return;
        }
        PaperRemnantStatus target = PaperRemnantStatus.fromProductionOrderStatus(orderStatus);
        List<PaperRemnant> remnants = remnantRepository.findBySourceProductionOrderId(
                companyId, productionOrderId.trim());
        for (PaperRemnant remnant : remnants) {
            if (remnant.getStatus() == target) {
                continue;
            }
            remnantRepository.save(remnant.withStatus(target, now));
        }
    }
}
