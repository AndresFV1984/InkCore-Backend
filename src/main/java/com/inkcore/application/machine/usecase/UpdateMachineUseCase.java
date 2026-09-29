package com.inkcore.application.machine.usecase;

import com.inkcore.domain.machine.exception.MachineAlreadyExistsException;
import com.inkcore.domain.machine.model.Machine;
import com.inkcore.domain.machine.model.MachineType;
import com.inkcore.domain.machine.ports.out.MachineRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class UpdateMachineUseCase {

    private final MachineRepositoryPort machineRepository;
    private final MachineSupport support;
    private final Clock clock;

    public UpdateMachineUseCase(MachineRepositoryPort machineRepository, MachineSupport support, Clock clock) {
        this.machineRepository = machineRepository;
        this.support = support;
        this.clock = clock;
    }

    @Transactional
    public Machine execute(UpdateMachineCommand command, Authentication authentication) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        Machine existing = machineRepository.findById(command.machineId())
                .orElseThrow(() -> new ResourceNotFoundException("MACHINE_NOT_FOUND", "Máquina no encontrada"));
        support.requireSameCompany(existing.getCompanyId(), companyId);
        String name = command.name() == null ? "" : command.name().trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (machineRepository.existsByCompanyIdAndNameIgnoreCaseExcludingId(companyId, name, existing.getMachineId())) {
            throw new MachineAlreadyExistsException(name);
        }
        Machine updated = existing.update(
                name,
                MachineType.fromApiValue(command.machineType()),
                command.manufacturer(),
                command.model(),
                command.purchaseCost(),
                command.usefulLifeYears(),
                command.annualMaintenanceCost(),
                command.monthlyOperatorCost(),
                command.energyCostPerHour(),
                command.productiveHoursPerYear(),
                command.state(),
                LocalDateTime.now(clock)
        );
        return machineRepository.save(updated, userId);
    }
}
