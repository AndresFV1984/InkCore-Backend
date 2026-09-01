package com.inkcore.application.supplier.usecase;

import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import com.inkcore.domain.supplier.model.Supplier;
import com.inkcore.domain.supplier.ports.out.SupplierRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetSupplierByIdUseCase {

    private final SupplierRepositoryPort supplierRepository;

    public GetSupplierByIdUseCase(SupplierRepositoryPort supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public Supplier execute(String supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SUPPLIER_NOT_FOUND",
                        "Proveedor no encontrado"
                ));
    }
}
