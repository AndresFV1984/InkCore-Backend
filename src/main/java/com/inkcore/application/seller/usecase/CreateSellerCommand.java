package com.inkcore.application.seller.usecase;

public record CreateSellerCommand(
        String companyId,
        String fullName,
        String documentType,
        String identification,
        String email,
        String phone,
        String department,
        String city,
        String address,
        Boolean state
) {
}
