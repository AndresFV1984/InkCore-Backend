package com.inkcore.application.client.usecase;

public record UpdateClientCommand(
        String clientId,
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
