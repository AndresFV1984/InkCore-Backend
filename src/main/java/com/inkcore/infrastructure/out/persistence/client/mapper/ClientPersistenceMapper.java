package com.inkcore.infrastructure.out.persistence.client.mapper;

import com.inkcore.domain.client.model.Client;
import com.inkcore.infrastructure.out.persistence.client.entity.ClientEntity;
import org.springframework.stereotype.Component;

@Component
public class ClientPersistenceMapper {

    public ClientEntity toNewEntity(Client client) {
        ClientEntity e = new ClientEntity();
        copyScalars(client, e);
        return e;
    }

    public void copyScalars(Client client, ClientEntity e) {
        e.setClientId(client.getClientId());
        e.setCompanyId(client.getCompanyId());
        e.setName(client.getName());
        e.setDocumentType(blankToNull(client.getDocumentType()));
        e.setIdentification(blankToNull(client.getIdentification()));
        e.setDepartment(client.getDepartment());
        e.setCity(client.getCity());
        e.setAddress(blankToNull(client.getAddress()));
        e.setPhone(blankToNull(client.getPhone()));
        e.setEmail(blankToNull(client.getEmail()));
        e.setContactPerson(blankToNull(client.getContactPerson()));
        e.setState(client.isState());
        e.setCreationDate(client.getCreationDate());
    }

    public Client toDomain(ClientEntity entity) {
        return Client.reconstitute(
                entity.getClientId(),
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
