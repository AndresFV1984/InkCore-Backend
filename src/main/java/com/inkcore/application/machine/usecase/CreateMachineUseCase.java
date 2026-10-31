package com.inkcore.application.machine.usecase;

import com.inkcore.domain.machine.exception.MachineAlreadyExistsException;
import com.inkcore.domain.machine.model.Machine;
import com.inkcore.domain.machine.model.MachineType;
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
        MachineType type = command.type();
        boolean state = Objects.requireNonNullElse(command.state(), true);
        boolean preferred = resolvePreferred(companyId, type, command.preferred(), state);
        Machine machine = Machine.createNew(
                companyId,
                name,
                type,
                command.manufacturer(),
                command.model(),
                command.purchaseCost(),
                command.usefulLifeYears(),
                command.annualMaintenanceCost(),
                command.monthlyOperatorCost(),
                command.energyCostPerHour(),
                command.productiveHoursPerYear(),
                command.setupMinutes(),
                preferred,
                state,
                LocalDate.now(clock),
                LocalDateTime.now(clock)
        );
        if (machine.isPreferred()) {
            machineRepository.clearPreferredForType(
                    companyId, type.getApiValue(), machine.getMachineId());
        }
        return machineRepository.save(machine, userId);
    }

    private boolean resolvePreferred(String companyId, MachineType type, Boolean requested, boolean state) {
        if (!state) {
            return false;
        }
        if (requested != null) {
            return requested;
        }
        // Primera del tipo → preferida (siempre hay un preferido por etapa cuando existe máquina).
        return !machineRepository.existsPreferredByCompanyIdAndMachineType(companyId, type.getApiValue());
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        return name.trim();
    }
}
