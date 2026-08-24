package com.inkcore.application.assemblyprice.usecase;

import com.inkcore.domain.assemblyprice.model.AssemblyPrice;
import com.inkcore.domain.assemblyprice.ports.out.AssemblyPriceRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAssemblyPriceByIdUseCase {

    private final AssemblyPriceRepositoryPort assemblyPriceRepository;

    public GetAssemblyPriceByIdUseCase(AssemblyPriceRepositoryPort assemblyPriceRepository) {
        this.assemblyPriceRepository = assemblyPriceRepository;
    }

    @Transactional(readOnly = true)
    public AssemblyPrice execute(String assemblyPriceId) {
        return assemblyPriceRepository.findById(assemblyPriceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ASSEMBLY_PRICE_NOT_FOUND",
                        "Precio de montaje no encontrado"
                ));
    }
}
