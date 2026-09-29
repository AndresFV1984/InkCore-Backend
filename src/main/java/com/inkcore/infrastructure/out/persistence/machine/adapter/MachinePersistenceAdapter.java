package com.inkcore.infrastructure.out.persistence.machine.adapter;

import com.inkcore.domain.machine.model.Machine;
import com.inkcore.domain.machine.model.MachineType;
import com.inkcore.domain.machine.ports.out.MachineRepositoryPort;
import com.inkcore.domain.shared.PageQuery;
import com.inkcore.domain.shared.PageResult;
import com.inkcore.infrastructure.out.persistence.machine.entity.MachineEntity;
import com.inkcore.infrastructure.out.persistence.machine.mapper.MachinePersistenceMapper;
import com.inkcore.infrastructure.out.persistence.machine.repository.JpaMachineRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class MachinePersistenceAdapter implements MachineRepositoryPort {

    private final JpaMachineRepository repository;
    private final MachinePersistenceMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    public MachinePersistenceAdapter(JpaMachineRepository repository, MachinePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Machine save(Machine machine, String changedBy) {
        bindSession(changedBy, false);
        MachineEntity existing = repository.findById(machine.getMachineId()).orElse(null);
        MachineEntity entity = existing == null ? mapper.toNewEntity(machine) : existing;
        if (existing != null) {
            mapper.copyScalars(machine, existing);
        }
        MachineEntity saved = repository.save(entity);
        entityManager.flush();
        entityManager.refresh(saved);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Machine> findById(String machineId) {
        return repository.findById(machineId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Machine> findPage(String companyId, Boolean state, String machineType, PageQuery pageQuery) {
        String type = machineType == null || machineType.isBlank()
                ? null
                : MachineType.fromApiValue(machineType).getApiValue();
        PageRequest pageable = PageRequest.of(pageQuery.page(), pageQuery.size(), Sort.by(Sort.Order.asc("name")));
        Page<MachineEntity> page;
        if (state != null && type != null) {
            page = repository.findAllByCompanyIdAndStateAndMachineType(companyId, state, type, pageable);
        } else if (state != null) {
            page = repository.findAllByCompanyIdAndState(companyId, state, pageable);
        } else if (type != null) {
            page = repository.findAllByCompanyIdAndMachineType(companyId, type, pageable);
        } else {
            page = repository.findAllByCompanyId(companyId, pageable);
        }
        return new PageResult<>(
                page.getContent().stream().map(mapper::toDomain).toList(),
                pageQuery.page(),
                pageQuery.size(),
                page.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndNameIgnoreCase(String companyId, String name) {
        return companyId != null && name != null
                && repository.existsByCompanyIdAndNameIgnoreCase(companyId.trim(), name.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCompanyIdAndNameIgnoreCaseExcludingId(String companyId, String name, String machineId) {
        return companyId != null && name != null && machineId != null
                && repository.existsByCompanyIdAndNameIgnoreCaseAndMachineIdNot(
                companyId.trim(), name.trim(), machineId);
    }

    @Override
    @Transactional
    public int snapshotActiveRates(String companyId, String changedBy) {
        bindSession(changedBy, true);
        return entityManager.createNativeQuery("""
                        UPDATE indicolors.machines
                        SET updated_at = now()
                        WHERE company_id = :companyId
                          AND state = TRUE
                        """)
                .setParameter("companyId", companyId)
                .executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findCompanyIdsWithActiveMachines() {
        return repository.findCompanyIdsWithActiveMachines();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Machine> findActiveByCompanyId(String companyId) {
        return repository.findAllByCompanyIdAndStateOrderByNameAsc(companyId, true).stream()
                .map(mapper::toDomain)
                .toList();
    }

    private void bindSession(String changedBy, boolean forceHistory) {
        entityManager.createNativeQuery("SELECT set_config('inkcore.changed_by', :changedBy, true)")
                .setParameter("changedBy", changedBy == null ? "" : changedBy)
                .getSingleResult();
        entityManager.createNativeQuery("SELECT set_config('inkcore.force_machine_cost_history', :force, true)")
                .setParameter("force", forceHistory ? "on" : "off")
                .getSingleResult();
    }
}
