package com.indicore.application.client.usecase;

/**
 * Comando de entrada para crear cliente (capa aplicación, sin anotaciones web).
 */
public record CreateClientCommand(
        String name,
        String nit,
        String phone,
        String city,
        String address,
        String email,
        String contact
) {
}
