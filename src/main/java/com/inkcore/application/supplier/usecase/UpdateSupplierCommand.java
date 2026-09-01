package com.inkcore.application.supplier.usecase;

public record UpdateSupplierCommand(
        String supplierId,
        String name,
        String documentType,
        String identification,
        String department,
        String city,
        String address,
        String phone,
        String email,
        String contactPerson,
        boolean state
) {
}
