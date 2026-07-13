package com.indicore.infrastructure.out.persistence.remission.adapter;

import com.indicore.domain.remission.model.Remission;
import com.indicore.domain.remission.ports.out.RemissionRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador temporal en memoria hasta implementar JPA + Flyway para remisiones.
 */
@Component
public class InMemoryRemissionPersistenceAdapter implements RemissionRepositoryPort {

    private final ConcurrentHashMap<UUID, Remission> store = new ConcurrentHashMap<>();

    @Override
    public Remission save(Remission remission) {
        store.put(remission.getId(), remission);
        return remission;
    }

    @Override
    public Optional<Remission> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Remission> findAll() {
        return new ArrayList<>(store.values());
    }
}
