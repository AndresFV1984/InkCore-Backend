package com.inkcore.application.seller.usecase;

import com.inkcore.domain.seller.exception.SellerAlreadyExistsException;
import com.inkcore.domain.seller.model.Seller;
import com.inkcore.domain.seller.ports.out.SellerRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateSellerUseCase {

    private final SellerRepositoryPort sellerRepository;

    public UpdateSellerUseCase(SellerRepositoryPort sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    @Transactional
    public Seller execute(UpdateSellerCommand command) {
        Seller existing = sellerRepository.findById(command.sellerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SELLER_NOT_FOUND",
                        "Vendedor no encontrado"
                ));

        String identification = requireTrimmed(
                command.identification(),
                "El número de identificación es obligatorio"
        );
        if (sellerRepository.existsByCompanyIdAndIdentificationIgnoreCaseExcludingSellerId(
                existing.getCompanyId(), identification, existing.getSellerId())) {
            throw new SellerAlreadyExistsException("identification", identification);
        }

        Seller updated = existing.update(
                command.fullName(),
                command.documentType(),
                identification,
                command.email(),
                command.phone(),
                command.department(),
                command.city(),
                command.address(),
                command.state()
        );
        return sellerRepository.save(updated);
    }

    private static String requireTrimmed(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
