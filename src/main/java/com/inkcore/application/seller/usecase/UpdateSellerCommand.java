package com.inkcore.application.seller.usecase;

public record UpdateSellerCommand(
        String sellerId,
        String fullName,
        String documentType,
        String identification,
        String email,
        String phone,
        String department,
        String city,
        String address,
        boolean state
) {
}
