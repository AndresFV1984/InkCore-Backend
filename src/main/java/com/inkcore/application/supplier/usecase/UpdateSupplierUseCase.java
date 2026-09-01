package com.inkcore.application.supplier.usecase;

import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import com.inkcore.domain.supplier.exception.SupplierAlreadyExistsException;
import com.inkcore.domain.supplier.model.Supplier;
import com.inkcore.domain.supplier.ports.out.SupplierRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateSupplierUseCase {

    private final SupplierRepositoryPort supplierRepository;

    public UpdateSupplierUseCase(SupplierRepositoryPort supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional
    public Supplier execute(UpdateSupplierCommand command) {
        Supplier existing = supplierRepository.findById(command.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SUPPLIER_NOT_FOUND",
                        "Proveedor no encontrado"
                ));

        String identification = blankToNull(command.identification());
        if (identification != null
                && supplierRepository.existsByCompanyIdAndIdentificationIgnoreCaseExcludingSupplierId(
                existing.getCompanyId(), identification, existing.getSupplierId())) {
            throw new SupplierAlreadyExistsException("identification", identification);
        }

        Supplier updated = existing.update(
                command.name(),
                command.documentType(),
                identification,
                command.department(),
                command.city(),
                command.address(),
                command.phone(),
                command.email(),
                command.contactPerson(),
                command.state()
        );
        return supplierRepository.save(updated);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
