package com.indicore.infrastructure.in.rest.clients;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClientRequest(
        @NotBlank(message = "El nombre o razón social es obligatorio")
        @Size(max = 200)
        String name,

        @Size(max = 50)
        String nit,

        @Size(max = 50)
        String phone,

        @Size(max = 100)
        String city,

        @Size(max = 300)
        String address,

        @Size(max = 150)
        String email,

        @Size(max = 150)
        String contact
) {
}
