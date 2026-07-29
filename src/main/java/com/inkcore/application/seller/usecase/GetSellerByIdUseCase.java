package com.inkcore.application.seller.usecase;

import com.inkcore.domain.seller.model.Seller;
import com.inkcore.domain.seller.ports.out.SellerRepositoryPort;
import com.inkcore.domain.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetSellerByIdUseCase {

    private final SellerRepositoryPort sellerRepository;

    public GetSellerByIdUseCase(SellerRepositoryPort sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    @Transactional(readOnly = true)
    public Seller execute(String sellerId) {
        return sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "SELLER_NOT_FOUND",
                        "Vendedor no encontrado"
                ));
    }
}
