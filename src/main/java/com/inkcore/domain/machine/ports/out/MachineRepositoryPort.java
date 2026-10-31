package com.inkcore.domain.machine.ports.out;

import com.inkcore.domain.machine.model.Machine;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;

import java.util.List;
import java.util.Optional;

public interface MachineRepositoryPort {

    Machine save(Machine machine, String changedBy);

    Optional<Machine> findById(String machineId);

    PageResult<Machine> findPage(String companyId, Boolean state, String machineType, PageQuery pageQuery);

    boolean existsByCompanyIdAndNameIgnoreCase(String companyId, String name);

    boolean existsByCompanyIdAndNameIgnoreCaseExcludingId(String companyId, String name, String machineId);

    /**
     * Dispara el trigger de historial sobre las máquinas activas de la empresa.
     * Devuelve cuántas filas se tocaron.
     */
    int snapshotActiveRates(String companyId, String changedBy);

    List<String> findCompanyIdsWithActiveMachines();

    List<Machine> findActiveByCompanyId(String companyId);

    /** Preferidas activas de la compañía (a lo sumo una por {@code machine_type}). */
    List<Machine> findPreferredActiveByCompanyId(String companyId);

    boolean existsPreferredByCompanyIdAndMachineType(String companyId, String machineType);

    /** Quita preferred de las demás del mismo tipo (salvo {@code excludeMachineId}). */
    int clearPreferredForType(String companyId, String machineType, String excludeMachineId);
}
