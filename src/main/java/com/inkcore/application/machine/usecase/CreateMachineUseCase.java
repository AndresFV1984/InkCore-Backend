package com.inkcore.application.machine.usecase;

import com.inkcore.domain.machine.exception.MachineAlreadyExistsException;
import com.inkcore.domain.machine.model.Machine;
import com.inkcore.domain.machine.ports.out.MachineRepositoryPort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class CreateMachineUseCase {

    private final MachineRepositoryPort machineRepository;
    private final MachineSupport support;
    private final Clock clock;

    public CreateMachineUseCase(MachineRepositoryPort machineRepository, MachineSupport support, Clock clock) {
        this.machineRepository = machineRepository;
        this.support = support;
        this.clock = clock;
    }

    @Transactional
    public Machine execute(CreateMachineCommand command, Authentication authentication) {
        String companyId = support.companyId(authentication);
        String userId = support.userId(authentication);
        String name = requireName(command.name());
        if (machineRepository.existsByCompanyIdAndNameIgnoreCase(companyId, name)) {
            throw new MachineAlreadyExistsException(name);
        }
        Machine machine = Machine.createNew(
                companyId,
                name,
                command.type(),
                command.manufacturer(),
                command.model(),
                command.purchaseCost(),
                command.usefulLifeYears(),
                command.annualMaintenanceCost(),
                command.monthlyOperatorCost(),
                command.energyCostPerHour(),
                command.productiveHoursPerYear(),
                Objects.requireNonNullElse(command.state(), true),
                LocalDate.now(clock),
                LocalDateTime.now(clock)
        );
        return machineRepository.save(machine, userId);
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        return name.trim();
    }
}
