package com.indicore.domain.client.ports.out;

import com.indicore.domain.client.model.Client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida: persistencia de clientes (implementado en infrastructure/out).
 */
public interface ClientRepositoryPort {

    Client save(Client client);

    Optional<Client> findById(UUID id);

    List<Client> findAll();

    boolean existsByNit(String nit);
}
