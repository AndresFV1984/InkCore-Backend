package com.inkcore.application.machine.usecase;

import com.inkcore.domain.machine.model.Machine;
import com.inkcore.domain.machine.ports.out.MachineRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListMachinesUseCase {

    private final MachineRepositoryPort machineRepository;
    private final MachineSupport support;

    public ListMachinesUseCase(MachineRepositoryPort machineRepository, MachineSupport support) {
        this.machineRepository = machineRepository;
        this.support = support;
    }

    @Transactional(readOnly = true)
    public PageResult<Machine> execute(Boolean state, String machineType, PageQuery pageQuery, Authentication authentication) {
        String companyId = support.companyId(authentication);
        PageQuery query = pageQuery == null ? PageQuery.of(0, PageQuery.DEFAULT_SIZE) : pageQuery;
        return machineRepository.findPage(companyId, state, machineType, query);
    }
}
