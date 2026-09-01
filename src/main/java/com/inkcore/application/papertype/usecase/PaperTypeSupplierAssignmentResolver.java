package com.inkcore.application.papertype.usecase;

import com.inkcore.domain.papertype.model.PaperTypeSupplierAssignment;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import com.inkcore.domain.supplier.model.Supplier;
import com.inkcore.domain.supplier.ports.out.SupplierRepositoryPort;

import java.util.ArrayList;
import java.util.List;

final class PaperTypeSupplierAssignmentResolver {

    private PaperTypeSupplierAssignmentResolver() {
    }

    static List<PaperTypeSupplierAssignment> resolve(
            String companyId,
            List<PaperTypeSupplierAssignmentCommand> commands,
            SupplierRepositoryPort supplierRepository
    ) {
        if (commands == null || commands.isEmpty()) {
            return List.of();
        }
        List<PaperTypeSupplierAssignment> resolved = new ArrayList<>();
        for (PaperTypeSupplierAssignmentCommand command : commands) {
            if (command == null || command.supplierId() == null || command.supplierId().isBlank()) {
                throw new IllegalArgumentException("El proveedor es obligatorio en las asignaciones");
            }
            if (command.sheetValue() == null) {
                throw new IllegalArgumentException("El valor de la hoja es obligatorio para cada proveedor");
            }
            if (command.packageUnit() == null) {
                throw new IllegalArgumentException("La unidad de empaque es obligatoria para cada proveedor");
            }
            Supplier supplier = supplierRepository.findById(command.supplierId().trim())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "SUPPLIER_NOT_FOUND",
                            "Proveedor no encontrado: " + command.supplierId()
                    ));
            if (!supplier.getCompanyId().equals(companyId)) {
                throw new IllegalArgumentException(
                        "El proveedor no pertenece a la misma empresa: " + command.supplierId()
                );
            }
            resolved.add(PaperTypeSupplierAssignment.of(
                    supplier.getSupplierId(),
                    command.sheetValue(),
                    command.packageUnit()
            ));
        }
        return resolved;
    }
}
