package com.inkcore.domain.supplier.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado proveedor. Campos alineados a {@code indicolors.suppliers}.
 */
public final class Supplier {

    private final String supplierId;
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

    private Supplier(
            String supplierId,
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
        this.supplierId = supplierId;
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

    public static Supplier createNew(
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

        return new Supplier(
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

    public Supplier update(
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

        return new Supplier(
                this.supplierId,
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

    public static Supplier reconstitute(
            String supplierId,
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
        return new Supplier(
                supplierId,
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

    public String getSupplierId() {
        return supplierId;
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
        if (!(o instanceof Supplier supplier)) return false;
        return Objects.equals(supplierId, supplier.supplierId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(supplierId);
    }
}
