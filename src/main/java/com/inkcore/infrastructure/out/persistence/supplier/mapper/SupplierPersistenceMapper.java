package com.inkcore.infrastructure.out.persistence.supplier.mapper;

import com.inkcore.domain.supplier.model.Supplier;
import com.inkcore.infrastructure.out.persistence.supplier.entity.SupplierEntity;
import org.springframework.stereotype.Component;

@Component
public class SupplierPersistenceMapper {

    public SupplierEntity toNewEntity(Supplier supplier) {
        SupplierEntity e = new SupplierEntity();
        copyScalars(supplier, e);
        return e;
    }

    public void copyScalars(Supplier supplier, SupplierEntity e) {
        e.setSupplierId(supplier.getSupplierId());
        e.setCompanyId(supplier.getCompanyId());
        e.setName(supplier.getName());
        e.setDocumentType(blankToNull(supplier.getDocumentType()));
        e.setIdentification(blankToNull(supplier.getIdentification()));
        e.setDepartment(supplier.getDepartment());
        e.setCity(supplier.getCity());
        e.setAddress(blankToNull(supplier.getAddress()));
        e.setPhone(blankToNull(supplier.getPhone()));
        e.setEmail(blankToNull(supplier.getEmail()));
        e.setContactPerson(blankToNull(supplier.getContactPerson()));
        e.setState(supplier.isState());
        e.setCreationDate(supplier.getCreationDate());
    }

    public Supplier toDomain(SupplierEntity entity) {
        return Supplier.reconstitute(
                entity.getSupplierId(),
                entity.getCompanyId(),
                entity.getName(),
                entity.getDocumentType(),
                entity.getIdentification(),
                entity.getDepartment(),
                entity.getCity(),
                entity.getAddress() == null ? "" : entity.getAddress(),
                entity.getPhone() == null ? "" : entity.getPhone(),
                entity.getEmail(),
                entity.getContactPerson() == null ? "" : entity.getContactPerson(),
                entity.isState(),
                entity.getCreationDate()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
