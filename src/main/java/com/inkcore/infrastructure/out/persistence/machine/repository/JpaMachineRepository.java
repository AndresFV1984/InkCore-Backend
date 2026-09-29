package com.inkcore.infrastructure.out.persistence.machine.repository;

import com.inkcore.infrastructure.out.persistence.machine.entity.MachineEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaMachineRepository extends JpaRepository<MachineEntity, String> {

    Page<MachineEntity> findAllByCompanyId(String companyId, Pageable pageable);

    Page<MachineEntity> findAllByCompanyIdAndState(String companyId, boolean state, Pageable pageable);

    Page<MachineEntity> findAllByCompanyIdAndMachineType(String companyId, String machineType, Pageable pageable);

    Page<MachineEntity> findAllByCompanyIdAndStateAndMachineType(
            String companyId,
            boolean state,
            String machineType,
            Pageable pageable
    );

    @Query("""
            SELECT CASE WHEN COUNT(m) > 0 THEN TRUE ELSE FALSE END
            FROM MachineEntity m
            WHERE m.companyId = :companyId
              AND LOWER(TRIM(m.name)) = LOWER(TRIM(:name))
            """)
    boolean existsByCompanyIdAndNameIgnoreCase(@Param("companyId") String companyId, @Param("name") String name);

    @Query("""
            SELECT CASE WHEN COUNT(m) > 0 THEN TRUE ELSE FALSE END
            FROM MachineEntity m
            WHERE m.companyId = :companyId
              AND LOWER(TRIM(m.name)) = LOWER(TRIM(:name))
              AND m.machineId <> :machineId
            """)
    boolean existsByCompanyIdAndNameIgnoreCaseAndMachineIdNot(
            @Param("companyId") String companyId,
            @Param("name") String name,
            @Param("machineId") String machineId
    );

    List<MachineEntity> findAllByCompanyIdAndStateOrderByNameAsc(String companyId, boolean state);

    @Query("SELECT DISTINCT m.companyId FROM MachineEntity m WHERE m.state = true")
    List<String> findCompanyIdsWithActiveMachines();
}
