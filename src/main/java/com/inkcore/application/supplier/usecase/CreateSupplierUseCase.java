package com.inkcore.application.supplier.usecase;

import com.inkcore.domain.supplier.exception.SupplierAlreadyExistsException;
import com.inkcore.domain.supplier.model.Supplier;
import com.inkcore.domain.supplier.ports.out.SupplierRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

@Service
public class CreateSupplierUseCase {

    private final SupplierRepositoryPort supplierRepository;
    private final Clock clock;

    public CreateSupplierUseCase(SupplierRepositoryPort supplierRepository, Clock clock) {
        this.supplierRepository = supplierRepository;
        this.clock = clock;
    }

    @Transactional
    public Supplier execute(CreateSupplierCommand command) {
        String identification = blankToNull(command.identification());
        if (identification != null
                && supplierRepository.existsByCompanyIdAndIdentificationIgnoreCase(
                command.companyId(), identification)) {
            throw new SupplierAlreadyExistsException("identification", identification);
        }

        boolean effectiveState = Objects.requireNonNullElse(command.state(), true);
        Supplier supplier = Supplier.createNew(
                command.companyId(),
                command.name(),
                command.documentType(),
                identification,
                command.department(),
                command.city(),
                command.address(),
                command.phone(),
                command.email(),
                command.contactPerson(),
                effectiveState,
                LocalDate.now(clock)
        );
        return supplierRepository.save(supplier);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
