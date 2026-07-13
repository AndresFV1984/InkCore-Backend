package com.indicore.domain.client.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio: cliente (sin dependencias de framework).
 */
public final class Client {

    private final UUID id;
    private final String name;
    private final String nit;
    private final String phone;
    private final String city;
    private final String address;
    private final String email;
    private final String contact;
    private final boolean active;

    private Client(
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
        this.id = id;
        this.name = name;
        this.nit = nit;
        this.phone = phone;
        this.city = city;
        this.address = address;
        this.email = email;
        this.contact = contact;
        this.active = active;
    }

    public static Client createNew(
            String name,
            String nit,
            String phone,
            String city,
            String address,
            String email,
            String contact
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre o razón social es obligatorio");
        }
        return new Client(
                UUID.randomUUID(),
                name.trim(),
                nullToEmpty(nit),
                nullToEmpty(phone),
                nullToEmpty(city).isBlank() ? "Medellín" : nullToEmpty(city),
                nullToEmpty(address),
                nullToEmpty(email),
                nullToEmpty(contact),
                true
        );
    }

    public static Client reconstitute(
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
        return new Client(id, name, nit, phone, city, address, email, contact, active);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNit() {
        return nit;
    }

    public String getPhone() {
        return phone;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public String getEmail() {
        return email;
    }

    public String getContact() {
        return contact;
    }

    public boolean isActive() {
        return active;
    }

    public boolean hasNit() {
        return nit != null && !nit.isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Client client)) return false;
        return Objects.equals(id, client.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
