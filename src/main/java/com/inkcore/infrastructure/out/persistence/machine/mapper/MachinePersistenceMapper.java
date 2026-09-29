package com.inkcore.infrastructure.out.persistence.machine.mapper;

import com.inkcore.domain.machine.model.Machine;
import com.inkcore.domain.machine.model.MachineType;
import com.inkcore.infrastructure.out.persistence.machine.entity.MachineEntity;
import org.springframework.stereotype.Component;

@Component
public class MachinePersistenceMapper {

    public MachineEntity toNewEntity(Machine machine) {
        MachineEntity entity = new MachineEntity();
        copyScalars(machine, entity);
        return entity;
    }

    public void copyScalars(Machine machine, MachineEntity entity) {
        entity.setMachineId(machine.getMachineId());
        entity.setCompanyId(machine.getCompanyId());
        entity.setName(machine.getName());
        entity.setMachineType(machine.getMachineType().getApiValue());
        entity.setManufacturer(machine.getManufacturer());
        entity.setModel(machine.getModel());
        entity.setPurchaseCost(machine.getPurchaseCost());
        entity.setUsefulLifeYears(machine.getUsefulLifeYears());
        entity.setAnnualMaintenanceCost(machine.getAnnualMaintenanceCost());
        entity.setMonthlyOperatorCost(machine.getMonthlyOperatorCost());
        entity.setEnergyCostPerHour(machine.getEnergyCostPerHour());
        entity.setProductiveHoursPerYear(machine.getProductiveHoursPerYear());
        entity.setState(machine.isState());
        entity.setCreationDate(machine.getCreationDate());
        entity.setUpdatedAt(machine.getUpdatedAt());
    }

    public Machine toDomain(MachineEntity entity) {
        return Machine.reconstitute(
                entity.getMachineId(),
                entity.getCompanyId(),
                entity.getName(),
                MachineType.fromApiValue(entity.getMachineType()),
                entity.getManufacturer(),
                entity.getModel(),
                entity.getPurchaseCost(),
                entity.getUsefulLifeYears(),
                entity.getAnnualMaintenanceCost(),
                entity.getMonthlyOperatorCost(),
                entity.getEnergyCostPerHour(),
                entity.getProductiveHoursPerYear(),
                entity.getCostPerHour(),
                entity.isState(),
                entity.getCreationDate(),
                entity.getUpdatedAt()
        );
    }
}
