package com.inkcore.application.seller.usecase;

import com.inkcore.domain.seller.exception.SellerAlreadyExistsException;
import com.inkcore.domain.seller.model.Seller;
import com.inkcore.domain.seller.ports.out.SellerRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

@Service
public class CreateSellerUseCase {

    private final SellerRepositoryPort sellerRepository;
    private final Clock clock;

    public CreateSellerUseCase(SellerRepositoryPort sellerRepository, Clock clock) {
        this.sellerRepository = sellerRepository;
        this.clock = clock;
    }

    @Transactional
    public Seller execute(CreateSellerCommand command) {
        String identification = requireTrimmed(command.identification(), "El número de identificación es obligatorio");
        if (sellerRepository.existsByCompanyIdAndIdentificationIgnoreCase(
                command.companyId(), identification)) {
            throw new SellerAlreadyExistsException("identification", identification);
        }

        boolean effectiveState = Objects.requireNonNullElse(command.state(), true);
        Seller seller = Seller.createNew(
                command.companyId(),
                command.fullName(),
                command.documentType(),
                identification,
                command.email(),
                command.phone(),
                command.department(),
                command.city(),
                command.address(),
                effectiveState,
                LocalDate.now(clock)
        );
        return sellerRepository.save(seller);
    }

    private static String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
