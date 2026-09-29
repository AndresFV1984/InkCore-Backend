package com.inkcore.application.machine.usecase;

import com.inkcore.domain.machine.model.Machine;
import com.inkcore.domain.machine.ports.out.MachineRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecalculateMachineRatesUseCase {

    private final MachineRepositoryPort machineRepository;
    private final MachineSupport support;

    public RecalculateMachineRatesUseCase(MachineRepositoryPort machineRepository, MachineSupport support) {
        this.machineRepository = machineRepository;
        this.support = support;
    }

    @Transactional
    public RecalculateMachineRatesResult execute(Authentication authentication) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        return snapshot(companyId, userId);
    }

    @Transactional
    public int executeForCompany(String companyId, String changedBy) {
        return machineRepository.snapshotActiveRates(companyId, changedBy);
    }

    private RecalculateMachineRatesResult snapshot(String companyId, String changedBy) {
        int count = machineRepository.snapshotActiveRates(companyId, changedBy);
        return new RecalculateMachineRatesResult(count, machineRepository.findActiveByCompanyId(companyId));
    }

    public record RecalculateMachineRatesResult(int machinesSnapshotted, List<Machine> machines) {
    }
}
