package com.inkcore.infrastructure.out.persistence.seller.mapper;

import com.inkcore.domain.seller.model.Seller;
import com.inkcore.infrastructure.out.persistence.seller.entity.SellerEntity;
import org.springframework.stereotype.Component;

@Component
public class SellerPersistenceMapper {

    public SellerEntity toNewEntity(Seller seller) {
        SellerEntity e = new SellerEntity();
        copyScalars(seller, e);
        return e;
    }

    public void copyScalars(Seller seller, SellerEntity e) {
        e.setSellerId(seller.getSellerId());
        e.setCompanyId(seller.getCompanyId());
        e.setFullName(seller.getFullName());
        e.setDocumentType(seller.getDocumentType());
        e.setIdentification(seller.getIdentification());
        e.setEmail(seller.getEmail());
        e.setPhone(blankToNull(seller.getPhone()));
        e.setDepartment(seller.getDepartment());
        e.setCity(seller.getCity());
        e.setAddress(blankToNull(seller.getAddress()));
        e.setState(seller.isState());
        e.setCreationDate(seller.getCreationDate());
    }

    public Seller toDomain(SellerEntity entity) {
        return Seller.reconstitute(
                entity.getSellerId(),
                entity.getCompanyId(),
                entity.getFullName(),
                entity.getDocumentType(),
                entity.getIdentification(),
                entity.getEmail(),
                entity.getPhone() == null ? "" : entity.getPhone(),
                entity.getDepartment(),
                entity.getCity(),
                entity.getAddress() == null ? "" : entity.getAddress(),
                entity.isState(),
                entity.getCreationDate()
        );
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
