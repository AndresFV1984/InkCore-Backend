package com.inkcore.application.client.usecase;

public record CreateClientCommand(
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
