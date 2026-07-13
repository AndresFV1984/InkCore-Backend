package com.indicore.infrastructure.out.persistence.client.repository;

import com.indicore.infrastructure.out.persistence.client.entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaClientRepository extends JpaRepository<ClientEntity, UUID> {

    boolean existsByNitIgnoreCase(String nit);
}
