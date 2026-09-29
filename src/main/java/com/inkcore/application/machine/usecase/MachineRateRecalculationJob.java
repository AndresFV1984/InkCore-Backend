package com.inkcore.application.machine.usecase;

import com.inkcore.domain.machine.ports.out.MachineRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Recálculo trimestral de tarifas. Deja un snapshot en machine_cost_history
 * aunque el costo/hora no haya cambiado ({@code changed_by} queda nulo).
 */
@Component
@ConditionalOnProperty(name = "inkcore.machines.rate-recalculation-enabled", havingValue = "true", matchIfMissing = true)
public class MachineRateRecalculationJob {

    private static final Logger log = LoggerFactory.getLogger(MachineRateRecalculationJob.class);

    private final MachineRepositoryPort machineRepository;
    private final RecalculateMachineRatesUseCase recalculateMachineRatesUseCase;

    public MachineRateRecalculationJob(
            MachineRepositoryPort machineRepository,
            RecalculateMachineRatesUseCase recalculateMachineRatesUseCase
    ) {
        this.machineRepository = machineRepository;
        this.recalculateMachineRatesUseCase = recalculateMachineRatesUseCase;
    }

    @Scheduled(cron = "${inkcore.machines.rate-recalculation-cron:0 0 3 1 1,4,7,10 *}")
    public void recalculateQuarterly() {
        for (String companyId : machineRepository.findCompanyIdsWithActiveMachines()) {
            try {
                int count = recalculateMachineRatesUseCase.executeForCompany(companyId, null);
                log.info("Recálculo trimestral de tarifas: companyId={} máquinas={}", companyId, count);
            } catch (RuntimeException ex) {
                log.warn("Recálculo trimestral de tarifas falló para companyId={}: {}", companyId, ex.getMessage());
            }
        }
    }
}
