package com.inkcore.domain.client.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado cliente. Campos alineados a {@code indicolors.clients}.
 */
public final class Client {

    private final String clientId;
    private final String companyId;
    private final String name;
    private final String documentType;
    private final String identification;
    private final String department;
    private final String city;
    private final String address;
    private final String phone;
    private final String email;
    private final String contactPerson;
    private final boolean state;
    private final LocalDate creationDate;

    private Client(
            String clientId,
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
            boolean state,
            LocalDate creationDate
    ) {
        this.clientId = clientId;
        this.companyId = companyId;
        this.name = name;
        this.documentType = documentType;
        this.identification = identification;
        this.department = department;
        this.city = city;
        this.address = address != null ? address : "";
        this.phone = phone != null ? phone : "";
        this.email = email;
        this.contactPerson = contactPerson != null ? contactPerson : "";
        this.state = state;
        this.creationDate = creationDate;
    }

    public static Client createNew(
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
            boolean state,
            LocalDate creationDate
    ) {
        requireNotBlank(companyId, "La empresa es obligatoria");
        requireNotBlank(name, "El nombre o razón social es obligatorio");
        requireNotBlank(department, "El departamento es obligatorio");
        requireNotBlank(city, "La ciudad / municipio es obligatorio");

        String normalizedEmail = blankToNull(email);
        if (normalizedEmail != null) {
            normalizedEmail = normalizedEmail.toLowerCase();
        }

        String normalizedDocType = blankToNull(documentType);
        if (normalizedDocType != null) {
            normalizedDocType = normalizedDocType.toUpperCase();
        }

        return new Client(
                UUID.randomUUID().toString(),
                companyId.trim(),
                name.trim(),
                normalizedDocType,
                blankToNull(identification),
                department.trim(),
                city.trim(),
                blankToEmpty(address),
                blankToEmpty(phone),
                normalizedEmail,
                blankToEmpty(contactPerson),
                state,
                creationDate
        );
    }

    public Client update(
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
        requireNotBlank(name, "El nombre o razón social es obligatorio");
        requireNotBlank(department, "El departamento es obligatorio");
        requireNotBlank(city, "La ciudad / municipio es obligatorio");

        String normalizedEmail = blankToNull(email);
        if (normalizedEmail != null) {
            normalizedEmail = normalizedEmail.toLowerCase();
        }

        String normalizedDocType = blankToNull(documentType);
        if (normalizedDocType != null) {
            normalizedDocType = normalizedDocType.toUpperCase();
        }

        return new Client(
                this.clientId,
                this.companyId,
                name.trim(),
                normalizedDocType,
                blankToNull(identification),
                department.trim(),
                city.trim(),
                blankToEmpty(address),
                blankToEmpty(phone),
                normalizedEmail,
                blankToEmpty(contactPerson),
                state,
                this.creationDate
        );
    }

    public static Client reconstitute(
            String clientId,
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
            boolean state,
            LocalDate creationDate
    ) {
        return new Client(
                clientId,
                companyId,
                name,
                documentType,
                identification,
                department,
                city,
                address,
                phone,
                email,
                contactPerson,
                state,
                creationDate
        );
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public String getClientId() {
        return clientId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getIdentification() {
        return identification;
    }

    public String getDepartment() {
        return department;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public boolean isState() {
        return state;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Client client)) return false;
        return Objects.equals(clientId, client.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId);
    }
}
