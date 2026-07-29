package com.inkcore.domain.seller.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Agregado vendedor. Campos alineados a {@code indicolors.sellers}.
 */
public final class Seller {

    private final String sellerId;
    private final String companyId;
    private final String fullName;
    private final String documentType;
    private final String identification;
    private final String email;
    private final String phone;
    private final String department;
    private final String city;
    private final String address;
    private final boolean state;
    private final LocalDate creationDate;

    private Seller(
            String sellerId,
            String companyId,
            String fullName,
            String documentType,
            String identification,
            String email,
            String phone,
            String department,
            String city,
            String address,
            boolean state,
            LocalDate creationDate
    ) {
        this.sellerId = sellerId;
        this.companyId = companyId;
        this.fullName = fullName;
        this.documentType = documentType;
        this.identification = identification;
        this.email = email;
        this.phone = phone != null ? phone : "";
        this.department = department;
        this.city = city;
        this.address = address != null ? address : "";
        this.state = state;
        this.creationDate = creationDate;
    }

    public static Seller createNew(
            String companyId,
            String fullName,
            String documentType,
            String identification,
            String email,
            String phone,
            String department,
            String city,
            String address,
            boolean state,
            LocalDate creationDate
    ) {
        requireNotBlank(companyId, "La empresa es obligatoria");
        requireNotBlank(fullName, "El nombre completo es obligatorio");
        requireNotBlank(documentType, "El tipo de documento es obligatorio");
        requireNotBlank(identification, "El número de identificación es obligatorio");
        requireNotBlank(email, "El correo electrónico es obligatorio");
        requireNotBlank(department, "El departamento es obligatorio");
        requireNotBlank(city, "La ciudad / municipio es obligatorio");

        return new Seller(
                UUID.randomUUID().toString(),
                companyId.trim(),
                fullName.trim(),
                documentType.trim().toUpperCase(),
                identification.trim(),
                email.trim().toLowerCase(),
                blankToEmpty(phone),
                department.trim(),
                city.trim(),
                blankToEmpty(address),
                state,
                creationDate
        );
    }

    public Seller update(
            String fullName,
            String documentType,
            String identification,
            String email,
            String phone,
            String department,
            String city,
            String address,
            boolean state
    ) {
        requireNotBlank(fullName, "El nombre completo es obligatorio");
        requireNotBlank(documentType, "El tipo de documento es obligatorio");
        requireNotBlank(identification, "El número de identificación es obligatorio");
        requireNotBlank(email, "El correo electrónico es obligatorio");
        requireNotBlank(department, "El departamento es obligatorio");
        requireNotBlank(city, "La ciudad / municipio es obligatorio");

        return new Seller(
                this.sellerId,
                this.companyId,
                fullName.trim(),
                documentType.trim().toUpperCase(),
                identification.trim(),
                email.trim().toLowerCase(),
                blankToEmpty(phone),
                department.trim(),
                city.trim(),
                blankToEmpty(address),
                state,
                this.creationDate
        );
    }

    public static Seller reconstitute(
            String sellerId,
            String companyId,
            String fullName,
            String documentType,
            String identification,
            String email,
            String phone,
            String department,
            String city,
            String address,
            boolean state,
            LocalDate creationDate
    ) {
        return new Seller(
                sellerId,
                companyId,
                fullName,
                documentType,
                identification,
                email,
                phone,
                department,
                city,
                address,
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

    public String getSellerId() {
        return sellerId;
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getIdentification() {
        return identification;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
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

    public boolean isState() {
        return state;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Seller seller)) return false;
        return Objects.equals(sellerId, seller.sellerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sellerId);
    }
}
