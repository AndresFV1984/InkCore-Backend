package com.indicore.infrastructure.out.persistence.order.adapter;

import com.indicore.domain.order.model.Order;
import com.indicore.domain.order.ports.out.OrderRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Adaptador temporal en memoria hasta implementar JPA + Flyway para órdenes.
 */
@Component
public class InMemoryOrderPersistenceAdapter implements OrderRepositoryPort {

    private final ConcurrentHashMap<UUID, Order> store = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        store.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Order> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Order> findByClientId(UUID clientId) {
        return store.values().stream()
                .filter(o -> o.getClientId().equals(clientId))
                .toList();
    }
}
