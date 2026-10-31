package com.inkcore.infrastructure.out.persistence.machine.repository;

import com.inkcore.infrastructure.out.persistence.machine.entity.MachineEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    List<MachineEntity> findAllByCompanyIdAndPreferredTrueAndStateTrueOrderByMachineTypeAsc(String companyId);

    boolean existsByCompanyIdAndMachineTypeAndPreferredTrue(String companyId, String machineType);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE MachineEntity m
            SET m.preferred = FALSE, m.updatedAt = CURRENT_TIMESTAMP
            WHERE m.companyId = :companyId
              AND m.machineType = :machineType
              AND m.preferred = TRUE
              AND m.machineId <> :excludeMachineId
            """)
    int clearPreferredForType(
            @Param("companyId") String companyId,
            @Param("machineType") String machineType,
            @Param("excludeMachineId") String excludeMachineId
    );

    @Query("SELECT DISTINCT m.companyId FROM MachineEntity m WHERE m.state = true")
    List<String> findCompanyIdsWithActiveMachines();
}
