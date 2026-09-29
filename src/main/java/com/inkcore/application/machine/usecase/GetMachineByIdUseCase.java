package com.inkcore.application.machine.usecase;

import com.inkcore.domain.machine.model.Machine;
import com.inkcore.domain.machine.ports.out.MachineRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetMachineByIdUseCase {

    private final MachineRepositoryPort machineRepository;
    private final MachineSupport support;

    public GetMachineByIdUseCase(MachineRepositoryPort machineRepository, MachineSupport support) {
        this.machineRepository = machineRepository;
        this.support = support;
    }

    @Transactional(readOnly = true)
    public Machine execute(String machineId, Authentication authentication) {
        String companyId = support.companyId(authentication);
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new ResourceNotFoundException("MACHINE_NOT_FOUND", "Máquina no encontrada"));
        support.requireSameCompany(machine.getCompanyId(), companyId);
        return machine;
    }
}
