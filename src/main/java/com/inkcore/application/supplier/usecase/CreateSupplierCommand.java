package com.inkcore.application.supplier.usecase;

public record CreateSupplierCommand(
        String companyId,
        String name,
        String documentType,
        String identification,
        String department,
        String city,
        String address,
        String phone,
        String email,
        String contactPerson,
        Boolean state
) {
}
