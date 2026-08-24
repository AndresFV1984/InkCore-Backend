package com.inkcore.application.thousandrate.usecase;

import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import com.inkcore.domain.thousandrate.model.ThousandRate;
import com.inkcore.domain.thousandrate.ports.out.ThousandRateRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetThousandRateByIdUseCase {

    private final ThousandRateRepositoryPort thousandRateRepository;

    public GetThousandRateByIdUseCase(ThousandRateRepositoryPort thousandRateRepository) {
        this.thousandRateRepository = thousandRateRepository;
    }

    @Transactional(readOnly = true)
    public ThousandRate execute(String thousandRateId) {
        return thousandRateRepository.findById(thousandRateId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "THOUSAND_RATE_NOT_FOUND",
                        "Tarifa por millar no encontrada"
                ));
    }
}
