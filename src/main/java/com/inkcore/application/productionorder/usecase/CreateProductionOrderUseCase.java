package com.inkcore.application.productionorder.usecase;

import com.inkcore.domain.client.ports.out.ClientRepositoryPort;
import com.inkcore.domain.machine.model.Machine;
import com.inkcore.domain.machine.ports.out.MachineRepositoryPort;
import com.inkcore.domain.productionorder.model.MachineUsage;
import com.inkcore.domain.productionorder.model.ProductionOrder;
import com.inkcore.domain.productionorder.model.ProductionOrderStage;
import com.inkcore.domain.productionorder.ports.out.MachineUsageRepositoryPort;
import com.inkcore.domain.seller.ports.out.SellerRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CreateProductionOrderUseCase {

    private final ProductionOrderSupport support;
    private final ClientRepositoryPort clientRepository;
    private final SellerRepositoryPort sellerRepository;
    private final MachineRepositoryPort machineRepository;
    private final MachineUsageRepositoryPort machineUsageRepository;

    public CreateProductionOrderUseCase(
            ProductionOrderSupport support,
            ClientRepositoryPort clientRepository,
            SellerRepositoryPort sellerRepository,
            MachineRepositoryPort machineRepository,
            MachineUsageRepositoryPort machineUsageRepository
    ) {
        this.support = support;
        this.clientRepository = clientRepository;
        this.sellerRepository = sellerRepository;
        this.machineRepository = machineRepository;
        this.machineUsageRepository = machineUsageRepository;
    }

    @Transactional
    public ProductionOrder execute(CreateProductionOrderCommand command, Authentication authentication) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);

        requireClient(command.clientId(), companyId);
        if (command.sellerId() != null && !command.sellerId().isBlank()) {
            requireSeller(command.sellerId(), companyId);
        }

        ProductionOrder order = ProductionOrder.createNew(
                companyId,
                support.nextOrderNumber(companyId),
                command.clientId(),
                command.workName(),
                command.sellerId(),
                command.orderDate(),
                command.requestedQuantity(),
                command.proposalQuantity1(),
                command.proposalQuantity2(),
                userId,
                support.now()
        );
        order.assignPriorityRank(support.repository().allocateNextPriorityRank(companyId));
        // Cobro queda fijo en quien crea la OP (administrador del JWT).
        order.upsertOperator(ProductionOrderStage.BILLING, userId);
        if (command.operatorUserId() != null && !command.operatorUserId().isBlank()) {
            order.upsertOperator(ProductionOrderStage.PREPRESS, command.operatorUserId());
        }
        ProductionOrder saved = support.repository().save(order);
        preloadPreferredMachines(saved);
        saved.setMachineUsages(
                machineUsageRepository.findByProductionOrderId(companyId, saved.getProductionOrderId()));
        return saved;
    }

    /**
     * Precarga la máquina preferida activa de cada etapa (setupMinutes del catálogo, run=0).
     */
    private void preloadPreferredMachines(ProductionOrder order) {
        List<Machine> preferred = machineRepository.findPreferredActiveByCompanyId(order.getCompanyId());
        if (preferred.isEmpty()) {
            return;
        }
        LocalDateTime now = support.now();
        for (Machine machine : preferred) {
            if (machine.getCostPerHour() == null) {
                continue;
            }
            MachineUsage usage = new MachineUsage();
            usage.setCompanyId(order.getCompanyId());
            usage.setProductionOrderId(order.getProductionOrderId());
            usage.setPhase(machine.getMachineType().getApiValue());
            usage.setMachineId(machine.getMachineId());
            usage.setMachineNameSnapshot(machine.getName());
            usage.setCostPerHourSnapshot(machine.getCostPerHour());
            usage.setEstimatedSetupMinutes(machine.getSetupMinutes());
            usage.setEstimatedRunMinutes(0);
            usage.setCreatedAt(now);
            usage.setUpdatedAt(now);
            machineUsageRepository.replacePhase(
                    order.getProductionOrderId(),
                    machine.getMachineType().getApiValue(),
                    List.of(usage)
            );
        }
    }

    private void requireClient(String clientId, String companyId) {
        var client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("CLIENT_NOT_FOUND", "Cliente no encontrado"));
        if (!companyId.equals(client.getCompanyId())) {
            throw new ResourceNotFoundException("CLIENT_NOT_FOUND", "Cliente no encontrado");
        }
    }

    private void requireSeller(String sellerId, String companyId) {
        var seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("SELLER_NOT_FOUND", "Vendedor no encontrado"));
        if (!companyId.equals(seller.getCompanyId())) {
            throw new ResourceNotFoundException("SELLER_NOT_FOUND", "Vendedor no encontrado");
        }
    }
}
