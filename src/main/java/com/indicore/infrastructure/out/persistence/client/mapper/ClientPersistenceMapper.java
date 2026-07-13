package com.indicore.infrastructure.out.persistence.client.mapper;

import com.indicore.domain.client.model.Client;
import com.indicore.infrastructure.out.persistence.client.entity.ClientEntity;
import org.springframework.stereotype.Component;

@Component
public class ClientPersistenceMapper {

    public ClientEntity toEntity(Client client) {
        ClientEntity entity = new ClientEntity();
        entity.setId(client.getId());
        entity.setName(client.getName());
        entity.setNit(emptyToNull(client.getNit()));
        entity.setPhone(emptyToNull(client.getPhone()));
        entity.setCity(emptyToNull(client.getCity()));
        entity.setAddress(emptyToNull(client.getAddress()));
        entity.setEmail(emptyToNull(client.getEmail()));
        entity.setContact(emptyToNull(client.getContact()));
        entity.setActive(client.isActive());
        return entity;
    }

    public Client toDomain(ClientEntity entity) {
        return Client.reconstitute(
                entity.getId(),
                entity.getName(),
                nullToEmpty(entity.getNit()),
                nullToEmpty(entity.getPhone()),
                nullToEmpty(entity.getCity()),
                nullToEmpty(entity.getAddress()),
                nullToEmpty(entity.getEmail()),
                nullToEmpty(entity.getContact()),
                entity.isActive()
        );
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
