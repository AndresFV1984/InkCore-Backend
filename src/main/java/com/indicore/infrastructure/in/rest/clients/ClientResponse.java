package com.indicore.infrastructure.in.rest.clients;

import com.indicore.domain.client.model.Client;

import java.util.UUID;

public record ClientResponse(
        UUID id,
        String name,
        String nit,
        String phone,
        String city,
        String address,
        String email,
        String contact,
        boolean active
) {
    public static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                displayOrDash(client.getNit()),
                displayOrDash(client.getPhone()),
                displayOrDash(client.getCity()),
                displayOrDash(client.getAddress()),
                displayOrDash(client.getEmail()),
                displayOrDash(client.getContact()),
                client.isActive()
        );
    }

    private static String displayOrDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
